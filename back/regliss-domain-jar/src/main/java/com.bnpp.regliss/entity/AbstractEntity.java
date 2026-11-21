package com.bnpp.regliss.entity;

// TODO: the project imports were not visible in the screenshots (the import block was collapsed in the IDE, shown only as "import ...").
// Re-add them in the IDE (Alt+Enter / Optimize Imports) for: Serializable, EntityUtils, and any other types used below.

/**
 * Base class for all entities.
 *
 * This class provides default implementations of the {@link #equals(Object)} and
 * {@link #hashCode()} operations. These implementations are based on the {@link EqualityField}
 * annotations. When no {@link EqualityField} are present on the concrete instance of this class,
 * the {@link #equals(Object)} and {@link #hashCode()} operations will use the
 * {@link Entity#getId()} to calculate the equality and hashCode.
 *
 * WARNING: It is the responsibility of the developer to make sure the ID of the Entity is always
 * set and will never change during the life cycle of an entity. If this cannot be guaranteed, the
 * developer should not use identity equality.
 *
 * TIP: If you use a UUID, as id, which is assigned on object creation, not when the object is
 * persisted, the previous problem is solved automatically.
 *
 * Thus, if you do not use an ID which is generated when the object is <b>created</b> ( like a
 * UUID) , you will have to use the {@link EqualityField} annotations in order to correctly
 * implement the {@link #equals(Object)} and {@link #hashCode()} operations, or you could implement
 * the {@link #equals(Object)} and {@link #hashCode()} operation manually.
 *
 */
public abstract class AbstractEntity<PK extends Serializable> implements Entity<PK> {

    /**
     * Calculate equality between to entities.
     *
     * This method implements the typical rules of object equality by comparing all fields that have
     * been annotated with {@link EqualityField}. If no fields are annotated as such
     * {@link AbstractEntity} <i>id</i> will be used for comparison.
     *
     * The following constraints do apply:
     * <ul>
     * <li>An object must be equal to itself.</li>
     * <li>An object can never be equal to null.</li>
     * <li>If two objects are equal their hash codes should be equal as well.</li>
     * <li>Transient fields must not be included for equality checking.</li>
     * <li>Static fields are not included for equality checking.</li>
     * </ul>
     *
     * @param other the other object to compare the entity to.
     * @return true if entities were found equal, false of not.
     */
    @Override
    public boolean equals(Object other) { return EntityUtils.isEqual(this, other); }

    /**
     * Returns a hash code value for the object. All fields annotated with {@link EqualityField}
     * will be included for the hash code calculation. If no such fields were found the
     * {@link AbstractEntity} <i>id</i> field will be used.
     *
     * @return the hash code for this entity.
     */
    @Override
    public int hashCode() { return EntityUtils.calculateHashCode(this); }
}
