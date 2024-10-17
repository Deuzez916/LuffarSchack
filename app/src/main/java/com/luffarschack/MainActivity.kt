@file:Suppress("DEPRECATION")

package com.luffarschack

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luffarschack.ui.theme.LuffarSchackTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuffarSchackTheme {
                LuffarSchackGame()
            }
        }
    }
}

@Composable
fun LuffarSchackGame() {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFBCFFDC)
    val textColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF3A3A3A)
    val buttonContainerColor = if (isDarkTheme) Color(0xFF424242) else Color(0xFF1C1C1C)
    val buttonTextColor = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFFFFD700)

    val context = LocalContext.current

    val initialStats = remember { PreferencesHelper.getLifetimeStats(context)}
    var gameStats by remember { mutableStateOf(GameStats(initialStats.first, initialStats.second, initialStats.third)) }

    var playerWins by remember { mutableStateOf(gameStats.playerWins) }
    var computerWins by remember { mutableStateOf(gameStats.computerWins) }
    var ties by remember { mutableStateOf(gameStats.ties) }

    var board by remember { mutableStateOf(List(9) { "" }) }
    var currentPlayer by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var isGameActive by remember { mutableStateOf(true) }
    var isPlayerTurn by remember { mutableStateOf(true) }

    fun resetGame() {
        board = List(9) { "" }
        currentPlayer = "X"
        winner = null
        isGameActive = true
        isPlayerTurn = true
    }

    fun handleWinOrTie(context: Context, winner: String?) {
        if (winner != null) {
            isGameActive = false
            if (winner == "X") {
                playerWins += 1
            } else if (winner == "O") {
                computerWins += 1
            }
        } else if (board.all { it.isNotEmpty() }) {
            isGameActive = false
            ties += 1
        }
        gameStats = GameStats(playerWins, computerWins, ties)

        PreferencesHelper.saveLifetimeStats(context, gameStats)
    }

    fun checkForWinner(b: List<String>): String? {
        val winningCombinations = listOf(
            listOf(0, 1, 2),
            listOf(3, 4, 5),
            listOf(6, 7, 8),
            listOf(0, 3, 6),
            listOf(1, 4, 7),
            listOf(2, 5, 8),
            listOf(0, 4, 8),
            listOf(2, 4, 6)
        )
        for (combo in winningCombinations) {
            if (b[combo[0]] == b[combo[1]] && b[combo[1]] == b[combo[2]] && b[combo[0]].isNotEmpty()) {
                return b[combo[0]]
            }
        }
        return null
    }

    fun getAvaliableCells(board: List<String>): List<Int> {
        return board.mapIndexedNotNull { index, value -> if (value.isEmpty()) index else null }
    }

    fun computerMoveEasy() {
        val avalibleCells = getAvaliableCells(board)
        if (avalibleCells.isNotEmpty()) {
            val randomMove = avalibleCells[Random.nextInt(avalibleCells.size)]
            board = board.toMutableList().also { it[randomMove] = "O"}
        }
    }

    fun computerMoveMedium() {
        val avaliableCells = getAvaliableCells(board)

        for (cell in avaliableCells) {
            val tempBoard = board.toMutableList().also { it[cell] = "O" }
            if (checkForWinner(tempBoard) == "O") {
                board = board.toMutableList().also { it[cell] = "O" }
                return
            }
        }

        for (cell in avaliableCells) {
            val tempBoard = board.toMutableList().also { it[cell] = "X" }
            if (checkForWinner(tempBoard) == "X") {
                board = board.toMutableList().also { it[cell] = "O" }
                return
            }
        }

        computerMoveEasy()
    }

    fun delayedComputerMove() {
        isPlayerTurn = false
        Handler(Looper.getMainLooper()).postDelayed({
            computerMoveMedium()
            winner = checkForWinner(board)
            if (winner != null || board.all { it.isNotEmpty() }) {
                handleWinOrTie(context, winner)
            }

            currentPlayer = "X"
            isPlayerTurn = true
        }, 500)
    }

    fun handlePlayerMove(index: Int, context: Context) {
        if (board[index].isEmpty() && isGameActive) {
            board = board.toMutableList().also { it[index] = currentPlayer }
            winner = checkForWinner(board)

            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }

            if (winner != null || board.all { it.isNotEmpty() }) {
                handleWinOrTie(context, winner)
            } else {
                currentPlayer = "O"
                delayedComputerMove()
            }
        }
    }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ){
        Row (
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 25.dp, end = 16.dp)
        ){
            DisplayWLTRatio(playerWins = playerWins, computerWins = computerWins, ties = ties, textColor)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp)
                .offset(y = ((-35).dp))
        ) {
            Text(
                text = "Player Wins: $playerWins",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier
                    .padding(20.dp, 0.dp, 0.dp, 8.dp)
                    .align(Alignment.Start)
            )
            Text(
                text = "Computer Wins: $computerWins",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier
                    .padding(20.dp, 0.dp, 0.dp, 8.dp)
                    .align(Alignment.Start)
            )
            Text(
                text = "Ties: $ties",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier
                    .padding(20.dp, 0.dp, 0.dp, 0.dp)
                    .align(Alignment.Start)
            )

            Board(board = board, onClick = { index -> handlePlayerMove(index, context) }, isPlayerTurn = isPlayerTurn)

            Spacer(modifier = Modifier.height(20.dp))

            if (winner != null) {
                Text(
                    text = "$winner Wins!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (!isGameActive) {
                Text(
                    text = "It´s a tie!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.padding(20.dp))

            if (!isGameActive) {
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainerColor,
                        contentColor = buttonTextColor
                    ),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Restart Game",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = buttonTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun Board(board: List<String>, onClick: (Int) -> Unit, isPlayerTurn: Boolean) {
    Column (modifier = Modifier.padding(16.dp)) {
        for (row in 0..2) {
            Row {
                for (col in 0..2) {
                    val index = row * 3 + col
                    GameCell(value = board[index], onClick = { onClick(index) }, isPlayerTurn = isPlayerTurn)
                }
            }
        }
    }
}

@Composable
fun GameCell(value: String, onClick: () -> Unit, isPlayerTurn: Boolean) {
    val alpha = animateFloatAsState(targetValue = if (value.isNotEmpty()) 1f else 0.5f)
    val scale = animateFloatAsState(targetValue = if (value.isNotEmpty()) 1f else 2f)
    val backgroundColor = when (value) {
        "X" -> Color(0XFFF8D7DA)
        "O" -> Color(0xFFD1E7DD)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
            .clickable(enabled = isPlayerTurn && value.isEmpty(), onClick = onClick)
            .alpha(alpha.value),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = if (value == "X") Color(0xFFD32F2F) else Color(0xFF1976D2),
            modifier = Modifier.scale(scale.value)
        )
    }
}

@Composable
fun DisplayWLTRatio(playerWins: Int, computerWins: Int, ties: Int, textColor: Color) {
    val winLossRatio = if (computerWins > 0) {
        playerWins.toDouble() / computerWins.toDouble()
    } else {
        playerWins.toDouble()
    }

    val totalGames = playerWins + computerWins + ties
    val tiePercentage = if (totalGames > 0) (ties.toDouble() / totalGames.toDouble()) * 100 else 0.0

    Column (
        modifier = Modifier.padding(top = 16.dp, end = 16.dp)
    ){
        Text(
            text = "W/L: %.2f".format(winLossRatio),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = "Ties: %.0f%%".format(tiePercentage),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LuffarSchackTheme {
        LuffarSchackGame()
    }
}