package de.mbrunner.inventory.movement;

public enum MovementType {
    /** Goods receipt: stock comes into a location (Wareneingang). */
    RECEIPT,
    /** Goods issue: stock leaves a location (Warenausgang). */
    ISSUE,
    /** Stock moves from one location to another (Umlagerung). */
    TRANSFER
}
