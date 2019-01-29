/*
 * Copyright 2019 Joao Pereira
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.jpereira.analyze

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.io.DirectoryChooser
import ij.measure.Measurements
import ij.measure.ResultsTable
import ij.plugin.filter.ParticleAnalyzer
import ij.plugin.frame.ThresholdAdjuster
import ij.process.AutoThresholder
import ij.process.ImageProcessor
import ij.process.StackProcessor
import loci.plugins.BF
import loci.plugins.`in`.ImporterOptions
import net.imagej.ops.OpService
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import java.awt.Dimension
import java.awt.Frame
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.Panel
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File

@Plugin(type = Command::class, menuPath = "Plugins>Analyze Images")
open class AnalyzeImage : Command {
    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var logService: OpService


    override fun run() {
        val directoryChooser = DirectoryChooser("Choose a directory to start processing")
        val directory = File(directoryChooser.directory)
        directory.listFiles().forEach {
            if (it.extension == "zvi") {
                val newWindow = TwoImageWindow()
                WindowManager.addWindow(newWindow)
                newWindow.startWindow(it.absolutePath)
            }
        }
    }
}

class TwoImageWindow : Frame("Two Images") {
    fun startWindow(imageFilename: String) {
        println("file: $imageFilename")
        val options = ImporterOptions()
        options.id = imageFilename
        options.isSplitChannels = true
        setSize(700, 600)
        layout = GridLayout(2, 2)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                // Handle the next set of images.....
            }
        })
        val originalImages = BF.openImagePlus(options)
        val images = BF.openImagePlus(options)

        println("number of channels: " + originalImages.size)

        val originalRedChannel = originalImages[0]
        val originalBlueChannel = originalImages.last()
        val redChannel = images[0]
        val blueChannel = images.last()

//        greenChannel.processor.invert()
        blueChannel.processor.blurGaussian(2.0)
        blueChannel.processor.invert()
        var redHistogram = redChannel.statistics.histogram
        redHistogram[redHistogram.lastIndex] = 0
        ThresholdAdjuster.setMode("B&W")
        val redChannelThreshold = AutoThresholder().getThreshold(AutoThresholder.Method.MaxEntropy, redHistogram)
        val blueChannelThreshold = AutoThresholder().getThreshold(AutoThresholder.Method.RenyiEntropy, blueChannel.statistics.histogram)
//        greenChannel.processor.scaleAndSetThreshold(0.0, greenChannelThreshold.toDouble(), ImageProcessor.RED_LUT)
        redChannel.processor.setThreshold(redChannelThreshold.toDouble(), 255.0, ImageProcessor.RED_LUT)
        blueChannel.processor.setThreshold(blueChannelThreshold.toDouble(), 255.0, ImageProcessor.RED_LUT)
//        blueChannel.processor.scaleAndSetThreshold(0.0, blueChannelThreshold.toDouble(), ImageProcessor.BLACK_AND_WHITE_LUT)

        redChannel.updateAndDraw()
        blueChannel.updateAndDraw()
        var table = ResultsTable()
        val redAnalyzer = ParticleAnalyzer(3, Measurements.CENTROID, table, 10.0, 9999999.0)
        redAnalyzer.analyze(redChannel)
        val blueAnalyzer = ParticleAnalyzer(3, Measurements.CENTROID, table, 20.0, 9999999.0)
        blueAnalyzer.analyze(redChannel)

        println("===================")
        table.headings.forEach { print(it) }
        println("===================")
        table.headings.forEach { print(it) }
        println("===================")

//        IJ.run(greenChannel, "Set Scale...", "known=10 pixel=10 unit=cm");
//        IJ.run(greenChannel, "Calibration Bar...", "location=[At Selection] fill=White label=Black number=5 decimal=0 font=14 zoom=1 overlay");
        //IJ.run(greenChannel, "8-bit", "")
        //IJ.run(greenChannel, "Auto Threshold", "method=[Triangle] white")


        resizeImage(originalRedChannel, 250, 250)
        resizeImage(originalBlueChannel, 250, 250)

        resizeImage(redChannel, 250, 250)
        resizeImage(blueChannel, 250, 250)

        add(ImagePanel(originalRedChannel))
        add(ImagePanel(redChannel))
        add(ImagePanel(originalBlueChannel))
        var blueChannelPanel = ImagePanel(blueChannel)
        blueChannelPanel.size = Dimension(250, 250)
        blueChannelPanel.bounds.size = Dimension(250, 250)
        add(blueChannelPanel)
        isVisible = true
    }
}

class ImagePanel internal constructor(private val img: ImagePlus) : Panel() {
    //    private val imageWidth: Int = img.width
    private val imageWidth: Int = 250
    //    private val imageHeight: Int = img.height
    private val imageHeight: Int = 250

    override fun getPreferredSize(): Dimension {
        return Dimension(imageWidth, imageHeight)
    }

    override fun getMinimumSize(): Dimension {
        return Dimension(imageWidth, imageHeight)
    }

    override fun paint(g: Graphics?) {
        g!!.drawImage(img.processor.createImage(), 0, 0, null)
    }
}


fun resizeImage(imp: ImagePlus, newWidth: Int, newHeight: Int) {
    println("width: ${imp.width}, height: ${imp.height}")
    val origWidth = imp.width
    val origHeight = imp.height
    try {
        val sp = StackProcessor(imp.stack, imp.processor)
        val s2 = sp.resize(newWidth, newHeight, true)
        val newSize = s2.size
        if (s2.width > 0 && newSize > 0) {
            val cal = imp.calibration
            if (cal.scaled()) {
                cal.pixelWidth *= origWidth / newWidth
                cal.pixelHeight *= origHeight / newHeight
            }
            imp.setStack(null, s2)
            imp.overlay = null
        }
    } catch (o: OutOfMemoryError) {
        IJ.outOfMemory("Resize")
    }

    imp.changes = true
}