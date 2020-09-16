# Change Log

## [unreleased] - yyyy-mm-dd
  
### Added
- added additional hint solver techniques
  - wings (xy-wing, xyz-wing)

### Changed
  
### Fixed
  

## [0.1] - 2020-09-15

Initial release of the sudoku solver as a Kotlin multiplatform library supporting
jvm and js targets.
 
### Added
- model classes for representing generic sudoku puzzles (Grid, House, Cell)
- brute force solver for sudokus
- hint solver for logical solving of sudokus, supporting the following techniques:
  - singles (full house, naked & hidden singles)
  - locked pairs / triples
  - intersections (pointing / claiming)
  - hidden pairs / triples / quadruples
  - naked pairs / triples / quadruples
  - basic fish (x-wing, swordfish, jellyfish)
  - single digit patterns (skyscraper, 2 string kite)
  - uniqueness tests (unique rectangle 1/2/4)
  - chains (remote pair, x-chain, xy-chain)
