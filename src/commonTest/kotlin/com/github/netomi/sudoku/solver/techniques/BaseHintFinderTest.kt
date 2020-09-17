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

import com.github.netomi.sudoku.Resource
import com.github.netomi.sudoku.io.GridValueLoader
import com.github.netomi.sudoku.model.Grid
import com.github.netomi.sudoku.model.PredefinedType
import com.github.netomi.sudoku.solver.AssignmentHint
import com.github.netomi.sudoku.solver.EliminationHint
import com.github.netomi.sudoku.solver.HintFinder
import com.github.netomi.sudoku.solver.HintSolver
import kotlin.test.*

abstract class BaseHintFinderTest
{
    internal abstract fun createHintFinder(): HintFinder

    internal abstract fun matches(testCase: TechniqueTestCase): Boolean

    private lateinit var testCases: List<TechniqueTestCase>

    @BeforeTest
    fun loadTests() {
        testCases = TestLoader.testCases
    }

    @Test
    fun testRegressionTests() {
        val hintFinder = createHintFinder()
        val solver = HintSolver(hintFinder)

        print("Executing testcases for technique: " + hintFinder.solvingTechnique.techniqueName + " - ")

        var count = 0
        for (testCase in testCases) {
            if (matches(testCase)) {
                val grid = Grid.of(PredefinedType.CLASSIC_9x9)
                grid.accept(GridValueLoader(testCase.givens))
                for (c in testCase.getDeletedCandidates()) {
                    val cell = grid.getCell(c.row, c.col)
                    cell.excludePossibleValues(false, c.value)
                }
                grid.updateState()

                val hints = solver.findAllHintsSingleStep(grid)
                var foundExpectedResult = false
                if (testCase.expectsDirectHint()) {
                    // Some heuristic to detect if a HintFinder would suddenly find too many hints.
                    assertTrue(hints.size() <= 10, "found " + hints.size() + " hints")
                    for (hint in hints) {
                        val result = (hint as AssignmentHint).description
                        if (result == testCase.placement!!.asPlacement()) {
                            foundExpectedResult = true
                            break
                        }
                    }
                } else {
                    // Some heuristic to detect if a HintFinder would suddenly find too many hints.
                    //assertTrue(hints.size() <= 10, "found " + hints.size() + " hints")
                    for (hint in hints) {
                        val candidateSet: MutableSet<Candidate> = HashSet()
                        candidateSet.addAll(testCase.getEliminations())
                        val eliminationHint = hint as EliminationHint
                        var index = 0
                        for (cell in eliminationHint.affectedCells.cells(grid)) {
                            for (excludedValue in eliminationHint.excludedValues[index].allSetBits()) {
                                val candidate = Candidate(cell.row.rowNumber,
                                                          cell.column.columnNumber,
                                                          excludedValue)
                                candidateSet.remove(candidate)
                            }
                            index++
                        }
                        if (candidateSet.isEmpty()) {
                            foundExpectedResult = true
                            break
                        }
                    }
                }
                if (foundExpectedResult) {
                    count++
                } else {
                    if (testCase.expectsDirectHint()) {
                        fail("Failed to find expected result '" + testCase.placement + "' in " + hints)
                    } else {
                        fail("Failed to find expected result '" + testCase.getEliminations() + "' in " + hints)
                    }
                }
            }
        }
        println("passed $count tests.")
    }

}

object TestLoader
{
    internal val testCases: MutableList<TechniqueTestCase> = ArrayList()

    init {
        val resource = Resource("reglib-1.4.txt")
        val text = resource.readText()

        for (line in text.split("\r\n")) {
            if (line.startsWith(":")) {
                testCases.add(TechniqueTestCase.of(line))
            }
        }
    }
}

internal class TechniqueTestCase private constructor(val technique: String,
                                                     private val candidate: String,
                                                     val givens: String,
                                                     deletedCandidatesString: String,
                                                     eliminationString: String?,
                                                     placementString: String?,
                                                     val extra: String?) {
    private val deletedCandidates: MutableList<Candidate>
    private var eliminations:      MutableList<Candidate> = mutableListOf()

    val placement: Candidate?

    fun expectsDirectHint(): Boolean {
        return placement != null
    }

    fun getDeletedCandidates(): Collection<Candidate> {
        return deletedCandidates
    }

    fun getEliminations(): Collection<Candidate> {
        return eliminations
    }

    override fun toString(): String {
        return "${technique}:${candidate}:${givens}:${deletedCandidates}:${eliminations}:${placement}"
    }

    companion object {
        fun of(line: String?): TechniqueTestCase {
            val tokens = line!!.split(":").toTypedArray()
            return TechniqueTestCase(tokens[1],
                tokens[2],
                tokens[3],
                tokens[4],
                if (tokens.size > 5) tokens[5] else null,
                if (tokens.size > 6) tokens[6] else null,
                if (tokens.size > 7) tokens[7] else null)
        }
    }

    init {
        placement = if (placementString != null && placementString.isNotEmpty()) Candidate.of(placementString) else null

        deletedCandidates = mutableListOf()
        for (str in deletedCandidatesString.split(" ").toTypedArray()) {
            if (str.isNotEmpty()) {
                deletedCandidates.add(Candidate.of(str))
            }
        }

        eliminationString?.let {
            for (str in it.split(" ").toTypedArray()) {
                if (str.isNotEmpty()) {
                    eliminations.add(Candidate.of(str))
                }
            }
        }
    }
}

class Candidate(val row: Int, val col: Int, val value: Int)
{
    fun asPlacement(): String {
        return "r${row}c${col}=${value}"
    }

    fun asElimination(): String {
        return toString()
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + col
        result = 31 * result + value
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Candidate) return false

        if (row   != other.row) return false
        if (col   != other.col) return false
        if (value != other.value) return false

        return true
    }

    override fun toString(): String {
        return "r${row}c${col}<>${value}"
    }

    companion object {
        fun of(str: String): Candidate {
            val row   = str[1].toString().toInt()
            val col   = str[2].toString().toInt()
            val value = str[0].toString().toInt()
            return Candidate(row, col, value)
        }
    }
}
