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

import com.github.netomi.sudoku.solver.SolvingTechnique.*

class UniqueRectangleType1Test : BaseHintFinderTest(UNIQUE_RECTANGLE_TYPE_1, "0600-[0-9]")

class UniqueRectangleType2Test : BaseHintFinderTest(UNIQUE_RECTANGLE_TYPE_2, "0601-[0-9]")

class UniqueRectangleType4Test : BaseHintFinderTest(UNIQUE_RECTANGLE_TYPE_4, "0603-[0-9]")
