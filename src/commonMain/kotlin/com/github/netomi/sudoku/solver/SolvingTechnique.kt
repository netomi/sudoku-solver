/*
 * Sudoku creator / solver / teacher.
 *
 * Copyright (c) 2020 Thomas Neidhart
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.github.netomi.sudoku.solver

import com.github.netomi.sudoku.solver.DifficultyLevel.*
import com.github.netomi.sudoku.solver.techniques.*
import kotlin.math.sign

enum class SolvingTechnique(val techniqueName:   String,
                            val difficultyLevel: DifficultyLevel,
                            val score:           Int,
                            val supplier:        () -> HintFinder)
{
    // Singles.
    FULL_HOUSE("Full House", EASY, 4, ::FullHouseFinder),
    NAKED_SINGLE("Naked Single", EASY, 4, ::NakedSingleFinder),
    HIDDEN_SINGLE("Hidden Single", EASY, 14, ::HiddenSingleFinder),

    // Locked subsets.
    LOCKED_PAIR("Locked Pair", MEDIUM, 40, ::LockedPairFinder),
    LOCKED_TRIPLE("Locked Triple", MEDIUM, 60, ::LockedTripleFinder),

    // Intersections.
    LOCKED_CANDIDATES_TYPE_1("Locked Candidates Type 1 (Pointing)", MEDIUM, 50, ::LockedCandidatesType1Finder),
    LOCKED_CANDIDATES_TYPE_2("Locked Candidates Type 2 (Claiming)", MEDIUM, 50, ::LockedCandidatesType2Finder),

    // Hidden subsets.
    HIDDEN_PAIR("Hidden Pair", MEDIUM, 70, ::HiddenPairFinder),
    HIDDEN_TRIPLE("Hidden Triple", MEDIUM, 100, ::HiddenTripleFinder),
    HIDDEN_QUADRUPLE("Hidden Quadruple", HARD, 150, ::HiddenQuadrupleFinder),

    // Naked subsets.
    NAKED_PAIR("Naked Pair", MEDIUM, 60, ::NakedPairFinder),
    NAKED_TRIPLE("Naked Triple", MEDIUM, 80, ::NakedTripleFinder),
    NAKED_QUADRUPLE("Naked Quadruple", HARD, 120, ::NakedQuadrupleFinder),

    // Basic fish.
    X_WING("X-Wing", HARD, 140, ::XWingHintFinder),
    SWORDFISH("Swordfish", HARD, 150, ::SwordFishFinder),
    JELLYFISH("Jellyfish", HARD, 160, ::JellyFishFinder),

    // Single digit patterns.
    SKYSCRAPER("Skyscraper", HARD, 130, ::SkyscraperFinder),
    TWO_STRING_KITE("2-String Kite", HARD, 150, ::TwoStringKiteFinder),

    // Uniqueness tests.
    UNIQUE_RECTANGLE_TYPE_1("Unique Rectangle Type 1", HARD, 100, ::UniqueRectangleType1Finder),
    UNIQUE_RECTANGLE_TYPE_2("Unique Rectangle Type 2", HARD, 100, ::UniqueRectangleType2Finder),
    // UNIQUE_RECTANGLE_TYPE_3("Unique Rectangle Type 3", HARD, 100, ::UniqueRectangleType3Finder),
    UNIQUE_RECTANGLE_TYPE_4("Unique Rectangle Type 4", HARD, 100, ::UniqueRectangleType4Finder),

    // Wings.
    XY_WING("XY-Wing", HARD, 160, ::XYWingFinder),
    XYZ_WING("XYZ-Wing", HARD, 180, ::XYZWingFinder),
    W_WING("W-Wing", HARD, 120, ::WWingFinder),

    // Chains.
    REMOTE_PAIR("Remote Pair", HARD, 110, ::RemotePairFinder),
    X_CHAIN("X-Chain", UNFAIR, 260, ::XChainFinder),
    XY_CHAIN("XY-Chain", UNFAIR, 260, ::XYChainFinder);
}

enum class DifficultyLevel {
    EASY,
    MEDIUM,
    HARD,
    UNFAIR,
    EXTREME
}

fun DifficultyLevel.max(other: DifficultyLevel): DifficultyLevel {
    return when((ordinal - other.ordinal).sign) {
        -1   -> other
        +1   -> this
        else -> this
    }
}