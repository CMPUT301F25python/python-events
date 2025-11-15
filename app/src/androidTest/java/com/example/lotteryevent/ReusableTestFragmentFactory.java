package com.example.lotteryevent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A reusable, generic FragmentFactory for testing.
 * This factory can be configured at runtime to provide specific instances of fragments.
 * It uses a Map to store "suppliers" (creation logic) for each Fragment class.
 * This avoids creating a separate TestFactory for every fragment in the app.
 */
public class ReusableTestFragmentFactory extends FragmentFactory {

    // A map to hold the "recipes" for creating fragments.
    // Key: The Fragment's class (e.g., HomeFragment.class)
    // Value: A function that knows how to create an instance of that fragment.
    private final Map<Class<? extends Fragment>, Supplier<Fragment>> fragmentSuppliers = new HashMap<>();

    /**
     * Registers a creation recipe for a specific fragment class.
     * @param clazz The class of the fragment to be created (e.g., HomeFragment.class).
     * @param supplier A lambda or function that returns a new instance of the fragment.
     * @param <T> The type of the fragment.
     */
    @SuppressWarnings("unchecked")
    public <T extends Fragment> void put(@NonNull Class<T> clazz, @NonNull Supplier<T> supplier) {
        fragmentSuppliers.put(clazz, (Supplier<Fragment>) supplier);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
        try {
            Class<? extends Fragment> fragmentClass = (Class<? extends Fragment>) Class.forName(className);
            Supplier<Fragment> supplier = fragmentSuppliers.get(fragmentClass);

            // If we have a recipe for this class, use it.
            if (supplier != null) {
                return supplier.get();
            }
            // Otherwise, let the default factory handle it.
            return super.instantiate(classLoader, className);
        } catch (ClassNotFoundException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className, e);
        }
    }
}