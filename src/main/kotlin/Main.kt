import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.gson.Gson
import data.LabeledData
import util.PointOperations
import neural.NeuralNetwork
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import ui.BasicAlertDialog
import data.Config
import java.io.FileReader

private const val TRAIN = 0
private const val PREDICT = 1

val signs = listOf("α", "β", "γ", "δ", "ε")

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun App() {

    MaterialTheme {
        val config = Gson().fromJson(FileReader("config.json"), Config::class.java).apply {
            layers[0] = representativePoints * 2
            layers[layers.lastIndex] = 5
        }

        var selectedMode by remember { mutableStateOf(TRAIN) }
        var selectedSign by remember { mutableStateOf(signs.firstOrNull()) }

        var counter by remember { mutableStateOf(0) }
        val pointsGroup = remember { mutableStateListOf<List<Offset>>() }
        val points = remember { mutableStateListOf<Offset>() }
        var lastPoint = remember { Offset.Unspecified }

        val model by remember { mutableStateOf(NeuralNetwork(config.layers)) }
        var data by remember { mutableStateOf<List<LabeledData>>(emptyList()) }
        var isTrained by remember { mutableStateOf(false) }
        var predictedSign by remember { mutableStateOf("") }

        var dialogMessage by remember { mutableStateOf("") }
        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            BasicAlertDialog("Info", dialogMessage) {
                showDialog = false
            }
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(4.dp)
                    .background(shape = RoundedCornerShape(16.dp), color = Color(0xFF374df5))
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    listOf(TRAIN, PREDICT).forEach { mode ->
                        val isSelected = selectedMode == mode
                        FilterChip(
                            shape = RoundedCornerShape(8.dp),
                            selected = isSelected,
                            enabled = mode != PREDICT || isTrained,
                            border = if (isSelected) BorderStroke(width = 2.dp, color = Color.Red) else null,
                            colors = ChipDefaults.filterChipColors(backgroundColor = Color.White, disabledBackgroundColor = Color.LightGray, selectedBackgroundColor = Color.White),
                            onClick = {
                                selectedMode = mode
                                predictedSign = ""
                                selectedSign = if (selectedMode == PREDICT) null else signs.first()
                                counter = 0
                                pointsGroup.clear()
                                points.clear()
                            }) {
                            Text(
                                text = if (mode == TRAIN) "Train" else "Predict",
                                fontSize = 16.sp,
                                color = if (isSelected) Color.Red else Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                if (selectedMode == TRAIN) {
                    Row {
                        Button(
                            onClick = {
                                selectedSign?.let {
                                    if (pointsGroup.size >= 20) {
                                        PointOperations.saveCoordinates(it, pointsGroup.toList())
                                    } else {
                                        dialogMessage = "Draw at least 20 samples for $selectedSign."
                                        showDialog = true
                                    }
                                }
                                pointsGroup.clear()
                                points.clear()
                                counter = 0
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Save", color = Color.Magenta)
                        }

                        Spacer(Modifier.width(4.dp))

                        Button(
                            onClick = { data = PointOperations.loadData(config.representativePoints) },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Load", color = Color.Magenta)
                        }

                        Spacer(Modifier.width(4.dp))

                        Button(
                            onClick = {
                                val X = mk.ndarray(data.map { it.values })
                                val y = mk.ndarray(data.map { it.label })
                                model.train(X.transpose(), y.transpose(), epochs = config.epochs, learningRate = config.learningRate, epsilon = config.epsilon)
                                isTrained = true
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, disabledBackgroundColor = Color.LightGray),
                            shape = RoundedCornerShape(16.dp),
                            enabled = data.isNotEmpty()
                        ) {
                            Text("Train", color = if (data.isNotEmpty()) Color.Magenta else Color.Black)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = counter.toString(),
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }
            }

            if (selectedMode == PREDICT) {
                Canvas(modifier = Modifier.weight(1f).fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            predictedSign = ""
                            pointsGroup.clear()
                            points.clear()
                            lastPoint = offset
                            points.add(offset)
                        },
                        onDragEnd = {
                            pointsGroup.add(points.toList())
                            val adjusted = PointOperations.adjustPoints(points)
                            val representatives = PointOperations.getRepresentativePoints(config.representativePoints, adjusted)
                            val X = mk.ndarray(listOf(representatives.flatMap { point -> listOf(point.x, point.y) }))
                            val predictions = model.predict(X.transpose())
                            predictedSign = signs[mk.math.argMax(predictions)]
                            println(signs[mk.math.argMax(predictions)])
                        }
                    ) { change, _ ->
                        val newPoint = change.position
                        points.addAll(PointOperations.interpolatePoints(lastPoint, newPoint))
                        lastPoint = newPoint
                    }
                }
                ) {
                    points.forEach { point ->
                        drawCircle(
                            color = Color.Magenta,
                            center = point,
                            radius = 1f,
                            style = Stroke(width = 1f)
                        )
                    }
                }
            } else {
                Canvas(modifier = Modifier.weight(1f).fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastPoint = offset
                            points.add(offset)
                        },
                        onDragEnd = {
                            pointsGroup.add(points.toList())
                            points.clear()
                            counter++
                        }
                    ) { change, _ ->
                        val newPoint = change.position
                        points.addAll(PointOperations.interpolatePoints(lastPoint, newPoint))
                        lastPoint = newPoint
                    }
                }
                ) {
                    points.forEach { point ->
                        drawCircle(
                            color = Color.Magenta,
                            center = point,
                            radius = 1f,
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(4.dp).background(shape = RoundedCornerShape(16.dp), color = Color(0xFF374df5)), horizontalArrangement = Arrangement.SpaceEvenly) {
                signs.forEach { sign ->
                    val isSelected = (selectedMode == TRAIN && selectedSign == sign) || (selectedMode == PREDICT && predictedSign == sign)
                    FilterChip(
                        selected = isSelected,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) BorderStroke(width = 2.dp, color = Color.Red) else null,
                        colors = ChipDefaults.filterChipColors(backgroundColor = Color.White, selectedBackgroundColor = Color.White),
                        onClick = {
                            selectedSign = sign
                            counter = 0
                            pointsGroup.clear()
                            points.clear()
                        }) {
                        Text(
                            text = sign,
                            fontSize = if (isSelected) 32.sp else 16.sp,
                            color = if (isSelected) Color.Red else Color(0xFF374df5)
                        )
                    }
                }
            }
        }
    }
}


fun main() = application {
    Window(
        title = "Slijepi Ahilej pogađa slova",
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}
