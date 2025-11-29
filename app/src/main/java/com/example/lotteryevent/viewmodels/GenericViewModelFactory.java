package com.example.lotteryevent.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A reusable, generic ViewModelProvider.Factory.
 *
 * This factory is a single, reusable class that can create any ViewModel. Instead of hardcoding
 * the creation logic, it holds a map of "suppliers" (creation recipes) that are provided at
 * runtime by the Fragment that needs the ViewModel.
 */
public class GenericViewModelFactory implements ViewModelProvider.Factory {

    // The "Cookbook": A map where each entry is a recipe for a specific ViewModel.
    // Key: The ViewModel's class (e.g., HomeViewModel.class).
    // Value: A function that, when called, creates a new instance of that ViewModel.
    private final Map<Class<? extends ViewModel>, Supplier<? extends ViewModel>> suppliers = new HashMap<>();

    /**
     * This is how a Fragment adds a "recipe" to the factory.
     * @param viewModelClass The class of the ViewModel this recipe is for.
     * @param supplier A lambda function that constructs the ViewModel, e.g., () -> new HomeViewModel(repo).
     */
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> void put(Class<T> viewModelClass, Supplier<T> supplier) {
        suppliers.put(viewModelClass, (Supplier<ViewModel>) supplier);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        // Find the recipe for the ViewModel class that was requested.
        Supplier<? extends ViewModel> supplier = suppliers.get(modelClass);

        // If we found a recipe, use it to create the ViewModel.
        if (supplier != null) {
            return (T) supplier.get();
        }

        // If no recipe was provided for this class, we can't create it.
        throw new IllegalArgumentException("No recipe found for ViewModel class: " + modelClass.getName());
    }
}