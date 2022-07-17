# Online-TicTacToe

Online tic-tac-toe game that allows the user to play against other users online.

# How to run

From CLI go to src folder and run command `java tictactoe.Client`, but before that make sure it is compiled with `javac tictactoe/*.java`

Currently, this works on localhost only so therefore in order to play the game, one of the users has to host the server by running
`java tictactoe.Server`

# TODO
* When user leaves the game, the other player can still make a move, therefore making an exception in communication with server. Fix that.
* Add a feature on the previous one, where a new user can join the lobby (the one that needs one more player because someone left)
* Make it work online. Need to learn how to do port forwarding (in Java?).

# NOTES
In app.config are constants for port and server host(which is indeed localhost currently). 
