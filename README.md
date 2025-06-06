# BattleshipProject
The goal of the project is to create a client of the app that can play the classic Battleship game using the server http://javaprojects.ch:50003 . The game was developed as part of an elective course called "Mobile Applications with Android."

## Team Members
Jibin Mathew Peechatt and Sameh Ahmed.

The contributions of each team member are written out in the code through comments, and the use of AI tools is mentioned directly in the code.

## Description
The Battleship Project is an Android version of the classic Battleship board game. The game features an easy-to-use, intuitive user interface that allows players to engage in strategic naval warfare against other players through a central server at http://javaprojects.ch:50003, which was developed by Prof. Dr. Bradley Richards. Players can randomly place their ships on a grid by clicking the "Generate Ships" button and then take turns trying to sink their opponents' fleets by guessing their locations.

Key features of our game:
- Interactive game board with touch controls
- Real-time multiplayer functionality via a server connection built by Bradley.
- Random ship placement with a regeneration option until players are satisfied
- Server connection testing capability (for testing purposes, to quickly check whether the server is running).
- Visual feedback for hits and misses
- Game state persistence

Visual representation of our game:

![](images/1.png)

Initially, the game will look like this when the emulator is running.

![](images/2.png)

The Start button would be enabled for players when the game key and player name have three letters and the ships have been placed in the grid using the Generate Ships button.

![](images/3.png)

When a player makes a move and hits an enemy ship, they are given information about whether they hit or sank the ship.

![](images/4.png)

Similarly, as shown in the above image, the other player/opponent would be notified that Player 1 hit their ship.