package com.optimizely.example;

/**
 * Product sorter
 **/

import com.optimizely.ab.config.parser.JsonParseException;
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.UpdateConfigNotification;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class App {
    private static final String DEBUG_TEXT_ON = "[DEBUG: Feature ON]";
    private static final String DEBUG_TEXT_OFF = "[DEBUG: Feature OFF]";

    static final String DATAFILE_URL = "https://cdn.optimizely.com/datafiles/FN6PNAH4J4dcZQDj6epN5.json";

    public static void main(String[] arguments) {
        Scanner scan = new Scanner(System.in);
        initializeOptimizely();
        scan.nextLine();
        System.exit(0);
    }

    private static void initializeOptimizely() {
        NotificationCenter notificationCenter = new NotificationCenter();

        // fetch any datafile changes, which result from configuration updates you make
        // to traffic percentage sliders, flag variable values, etc.
        HttpProjectConfigManager configManager = HttpProjectConfigManager
                .builder().withUrl(DATAFILE_URL)
                .withPollingInterval((long) 5, TimeUnit.SECONDS)
                .withNotificationCenter(notificationCenter)
                .build(false);

        Optimizely optimizely = Optimizely.builder()
                .withConfigManager(configManager)
                .withNotificationCenter(notificationCenter)
                .build();

        // notification listener listens and responds to config updates that occur when
        // user makes changes
        // in the tutorial by interacting with the UI
        notificationCenter.addNotificationHandler(UpdateConfigNotification.class,
                x -> runProductSorter(optimizely));

        runProductSorter(optimizely);
    }

    private static void runProductSorter(Optimizely optimizely) {

        int rangeMax = 999999;
        int rangeMin = 100000;
        ArrayList<String> userIds = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            userIds.add((random.nextInt(rangeMax - rangeMin) + rangeMin) + "");
        }

        System.out.println("\n\nWelcome to our product catalog!");
        System.out.println("Let's see what product sorting the visitors experience!\n");

        // For each visitor, let's see what experience they get
        ArrayList<Experience> experienceList = new ArrayList<>();
        Map<String, Integer> frequecyMap = new HashMap<>();
        int onVariationsCount = 0;
        for (String userId : userIds) {
            Experience experience = getExperience(optimizely, userId);
            experienceList.add(experience);

            // Count how many visitors had the flag (not feature) enabled
            onVariationsCount += experience.isEnabled ? 1 : 0;

            // Count what experience each visitor got
            if (experience.text != null && !experience.text.isEmpty()) {
                frequecyMap.put(experience.text,
                        frequecyMap.containsKey(experience.text) ? frequecyMap.get(experience.text) + 1 : 1);
            }
        }

        printLines(experienceList, onVariationsCount > 0);

        int totalCount = userIds.size();

        long percentage = Math.round((double) onVariationsCount / totalCount * 100);

        if (onVariationsCount > 0) {
            System.out.println("\n" + onVariationsCount + " out of " + totalCount + " visitors (~" + percentage
                    + "%) had the feature enabled.\n");

            for (Map.Entry<String, Integer> experience : frequecyMap.entrySet()) {
                Integer count = experience.getValue();
                long experiencePercentage = Math.round((double) count / totalCount * 100);
                System.out.println(experience.getValue() + " visitors (~" + experiencePercentage
                        + "%) got the experience: '" + experience.getKey() + "'");
            }
        }
    }

    private static void printLines(ArrayList<Experience> experienceList, boolean isDebug) {
        for (int index = 0; index < experienceList.size(); index++) {
            Experience experience = experienceList.get(index);
            String userId = "Visitor #" + index + ": ";
            String line = isDebug ? experience.debugText + " " + experience.text : experience.text;
            System.out.println(userId + line);
        }
    }

    private static Experience getExperience(Optimizely optimizely, String userId) {
        String text = "";
        boolean purchasedItem = false;
        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide("product_sort");

        // the text that is printed comes from feature variables
        if (decision.getEnabled()) {
            try {
                // mocks config values with print statements like "Variation 1 shows products
                // sorted by popularity!"
                text = decision.getVariables().getValue("sort_method", String.class);
            } catch (JsonParseException e) {
                e.printStackTrace();
            }
        } else {
            // default fallback if flag off for user
            text = "Flag off. User saw the product list sorted alphabetically by default.";
        }
        return new Experience(text, decision.getEnabled(), decision.getEnabled() ? DEBUG_TEXT_ON : DEBUG_TEXT_OFF);
    }

    public static class Experience {
        String text;
        Boolean isEnabled;
        String debugText;

        Experience(String text, Boolean isEnabled, String debugText) {
            this.text = text;
            this.isEnabled = isEnabled;
            this.debugText = debugText;
        }
    }
}
