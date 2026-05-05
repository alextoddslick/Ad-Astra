package earth.terrarium.adastra.common.utils;

import earth.terrarium.common_storage_lib.storage.base.ValueStorage;

import java.util.function.Supplier;

/**
 * External-facing wrapper around a {@link ValueStorage} that hides one of the
 * insert/extract directions. Used so cables and adjacent blocks see the right
 * "shape" of a machine's energy storage (consumers reject extraction; generators
 * reject insertion) without changing how the machine accesses its own storage
 * internally.
 */
public class FilteredEnergyView implements ValueStorage {

    private final Supplier<? extends ValueStorage> delegate;
    private final boolean allowInsert;
    private final boolean allowExtract;

    private FilteredEnergyView(Supplier<? extends ValueStorage> delegate, boolean allowInsert, boolean allowExtract) {
        this.delegate = delegate;
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
    }

    public static FilteredEnergyView insertOnly(Supplier<? extends ValueStorage> delegate) {
        return new FilteredEnergyView(delegate, true, false);
    }

    public static FilteredEnergyView extractOnly(Supplier<? extends ValueStorage> delegate) {
        return new FilteredEnergyView(delegate, false, true);
    }

    @Override
    public long getStoredAmount() {
        return delegate.get().getStoredAmount();
    }

    @Override
    public long getCapacity() {
        return delegate.get().getCapacity();
    }

    @Override
    public boolean allowsInsertion() {
        return allowInsert;
    }

    @Override
    public boolean allowsExtraction() {
        return allowExtract;
    }

    @Override
    public long insert(long amount, boolean simulate) {
        if (!allowInsert) return 0;
        return delegate.get().insert(amount, simulate);
    }

    @Override
    public long extract(long amount, boolean simulate) {
        if (!allowExtract) return 0;
        return delegate.get().extract(amount, simulate);
    }
}
