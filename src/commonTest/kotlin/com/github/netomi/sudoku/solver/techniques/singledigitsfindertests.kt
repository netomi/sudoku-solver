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
package com.github.netomi.sudoku.solver.techniques

import com.github.netomi.sudoku.solver.HintFinder

class SkyscraperFinderTest : BaseHintFinderTest() {
    override fun createHintFinder(): HintFinder {
        return SkyscraperFinder()
    }

    override fun matches(testCase: TechniqueTestCase): Boolean {
        return testCase.technique.startsWith("0400")
    }
}

class TwoStringKiteFinderTest : BaseHintFinderTest() {
    override fun createHintFinder(): HintFinder {
        return TwoStringKiteFinder()
    }

    override fun matches(testCase: TechniqueTestCase): Boolean {
        return testCase.technique.startsWith("0401")
    }
}

class EmptyRectangleFinderTest : BaseHintFinderTest() {
    override fun createHintFinder(): HintFinder {
        return EmptyRectangleFinder()
    }

    override fun matches(testCase: TechniqueTestCase): Boolean {
        return testCase.technique.startsWith("0402")
    }
}

class TurbotFishFinderTest : BaseHintFinderTest() {
    override fun createHintFinder(): HintFinder {
        return TurbotFishFinder()
    }

    override fun matches(testCase: TechniqueTestCase): Boolean {
        return testCase.technique.startsWith("0403")
    }
}
