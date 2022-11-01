package org.calyxos.bellis

enum class GarlicLevel(val value: Int) {
    STANDARD(0),
    SAFER(1),
    SAFEST(2);

    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.value == value }
    }
}