package com.coxgrind.track;

/**
 * Olm body-part ids. Right claw is the mage hand. Left claw is the melee hand.
 * The second id of each pair is the Challenge Mode form.
 */
public final class OlmNpcs
{
	public static final int ENCOUNTER_OBJECT = 29881;

	private static final int MAGE_HAND = 7550;
	private static final int MAGE_HAND_CM = 7553;
	private static final int MELEE_HAND = 7552;
	private static final int MELEE_HAND_CM = 7555;

	private OlmNpcs()
	{
	}

	public static boolean isMageHand(int id)
	{
		return id == MAGE_HAND || id == MAGE_HAND_CM;
	}

	public static boolean isMeleeHand(int id)
	{
		return id == MELEE_HAND || id == MELEE_HAND_CM;
	}
}
