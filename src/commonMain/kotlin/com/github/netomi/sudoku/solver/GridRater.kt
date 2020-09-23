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

import com.github.netomi.sudoku.model.Grid

object GridRater {
    private val hintSolver: HintSolver

    fun rate(grid: Grid): Pair<DifficultyLevel, Int> {
        val hints = hintSolver.findAllHints(grid)

        var difficulty = DifficultyLevel.EASY
        var score      = 0

        for (hint in hints) {
            score += hint.solvingTechnique.score
            difficulty = difficulty.max(hint.solvingTechnique.difficultyLevel)
        }

        return Pair(difficulty, score)
    }

    init {
        // sort the hint finders by score in order to find hints with a smaller score first.
        // there is no guarantee that the returned score is the lowest possible.
        val orderedFinders = SolvingTechnique.values().sortedWith { t1, t2 -> t1.score - t2.score }
        hintSolver = HintSolver(orderedFinders)
    }
}