package com.airmap.airmapsdk.controllers;

import android.support.annotation.NonNull;

import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdk.util.CopyCollections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

import static com.airmap.airmapsdk.models.rules.AirMapRuleset.Type.PickOne;

public class RulesetsEvaluator {

    public static List<AirMapRuleset> computeSelectedRulesets(@NonNull List<AirMapRuleset> availableRulesets, @NonNull AirMapMapView.Configuration configuration) {
        switch (configuration.type) {
            case AUTOMATIC:
                return computeSelectedRulesets(availableRulesets, new HashSet<String>(), new HashSet<String>(), true);
            case DYNAMIC:
                AirMapMapView.DynamicConfiguration dynamicConfig = (AirMapMapView.DynamicConfiguration) configuration;
                HashSet<String> preferredRulesets = new HashSet<>(dynamicConfig.preferredRulesetIds);
                HashSet<String> unpreferredRulesets = new HashSet<>(dynamicConfig.unpreferredRulesetIds);

                return computeSelectedRulesets(availableRulesets, preferredRulesets, unpreferredRulesets, dynamicConfig.enableRecommendedRulesets);
            case MANUAL:
                return ((AirMapMapView.ManualConfiguration) configuration).selectedRulesets;
        }

        return null;
    }

    /**
     * Calculate selected rulesets from rulesets available and preferred/unpreferred rulesets
     *
     * @param availableRulesets   - All potential rulesets for the given jurisdictions
     * @param preferredRulesets   - Optional or Pick-One rulesets that the user has manually enabled
     * @param unpreferredRulesets - Optional rulesets that the user has manually disabled
     * @return selectedRulesets
     */
    private static List<AirMapRuleset> computeSelectedRulesets(@NonNull List<AirMapRuleset> availableRulesets, @NonNull Set<String> preferredRulesets, @NonNull Set<String> unpreferredRulesets, boolean enableRecommendedRulesets) {
        // select rulesets that are preferred, required or default
        Set<AirMapRuleset> selectedRulesets = new HashSet<>();
        for (AirMapRuleset newRuleset : CopyCollections.copy(availableRulesets)) {
            // preferred are automatically added
            if (preferredRulesets.contains(newRuleset.getId())) {
                // unselect sibling rulesets
                if (newRuleset.getType() == PickOne) {
                    for (AirMapRuleset selectedRuleset : CopyCollections.copy(selectedRulesets)) {
                        if (selectedRuleset.getType() == PickOne && selectedRuleset.getJurisdiction().getId() == newRuleset.getJurisdiction().getId()) {
                            selectedRulesets.remove(selectedRuleset);
                        }
                    }
                }

                selectedRulesets.add(newRuleset);
                continue;
            }

            switch (newRuleset.getType()) {
                case Optional: {
                    // select optional rulesets that default on and haven't been manually deselected by user
                    if (newRuleset.isDefault() && !unpreferredRulesets.contains(newRuleset.getId()) && enableRecommendedRulesets) {
                        selectedRulesets.add(newRuleset);
                    }
                    break;
                }
                case Required: {
                    // required are required :P
                    selectedRulesets.add(newRuleset);
                    break;
                }
                case PickOne: {
                    // select pick-one's that default on and don't have a sibling that is preferred or selected
                    if (newRuleset.isDefault()) {
                        boolean siblingsSelected = false;
                        // check if there's a sibling selected or preferred (only pick-one's have siblings)
                        for (AirMapRuleset selectedRuleset : selectedRulesets) {
                            if (selectedRuleset.getType() == PickOne && selectedRuleset.getJurisdiction().getId() == newRuleset.getJurisdiction().getId()) {
                                siblingsSelected = true;
                                break;
                            }
                        }

                        // if not, we can select this ruleset
                        if (!siblingsSelected) {
                            selectedRulesets.add(newRuleset);
                        }
                    }
                    break;
                }
            }
        }

        // Sort rulesets by type (pick one > optional > required) then alphabetically
        List<AirMapRuleset> selectedRulesetsList = new ArrayList<>(selectedRulesets);
        Collections.sort(selectedRulesetsList, new Comparator<AirMapRuleset>() {
            @Override
            public int compare(AirMapRuleset o1, AirMapRuleset o2) {
                return o1.compareTo(o2);
            }
        });
        Timber.v("Rulesets evaluated: %s", selectedRulesets);

        return selectedRulesetsList;
    }

    private static boolean checkIfRulesetsHaveChanged(List<AirMapRuleset> oldSelectedRulesets, List<AirMapRuleset> newSelectedRulesets) {
        for (AirMapRuleset newRuleset : newSelectedRulesets) {
            if (!oldSelectedRulesets.contains(newRuleset)) {
                return true;
            }
        }

        for (AirMapRuleset oldRuleset : oldSelectedRulesets) {
            if (!newSelectedRulesets.contains(oldRuleset)) {
                return true;
            }
        }

        return false;
    }
}
