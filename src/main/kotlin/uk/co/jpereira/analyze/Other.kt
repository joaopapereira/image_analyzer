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

import net.imagej.display.ImageDisplay
import net.imagej.display.ImageDisplayService
import net.imagej.ops.OpService
import net.imagej.ops.convert.RealTypeConverter
import net.imglib2.img.Img
import net.imglib2.type.NativeType
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.real.FloatType
import org.scijava.ItemIO
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin

/**
 * This tutorial shows how to use the ImageJ Ops DoG filter, and how to use
 * ImageJ Ops normalizeScale op to do image type conversion.
 *
 *
 * A main method is provided so this class can be run directly from Eclipse (or
 * any other IDE).
 *
 *
 *
 * Also, because this class implements [Command] and is annotated as an
 * `@Plugin`, it will show up in the ImageJ menus: under Tutorials&gt;DoG
 * Filtering, as specified by the `menuPath` field of the `@Plugin`
 * annotation.
 *
 */
@Plugin(type = Command::class, menuPath = "Tutorials>DoG Filtering")
class UsingOpsDog<T : RealType<T>> : Command where T : NativeType<T> {

    /*
     * This {@code @Parameter} is for Image Display service.
     * The context will provide it automatically when this command is created.
     */
    @Parameter
    private val imageDisplayService: ImageDisplayService? = null

    /*
     * This {@code @Parameter} is for ImageJ Ops service. The
     * context will provide it automatically when this command is created.
     */
    @Parameter
    private val opService: OpService? = null

    @Parameter(type = ItemIO.INPUT)
    private val displayIn: ImageDisplay? = null

    /*
     * This command will produce an image that will automatically be shown by
     * the framework. Again, this command is "UI agnostic": how the image is
     * shown is not specified here.
     */
    //@Parameter(type = ItemIO.INPUT)
    //private Dataset imageDataset;
    @Parameter(type = ItemIO.OUTPUT)
    private var output: Img<T>? = null

    /*
     * The run() method is where we do the actual 'work' of the command. In this
     * case, it is fairly trivial because we are simply calling ImageJ Services.
     */
    override fun run() {
        val input = imageDisplayService!!.getActiveDataset(displayIn)
        val image = input.imgPlus as Img<T>

        // Convert image to FloatType for better numeric precision
        val converted = opService!!.convert().float32(image)

        // Create the filtering result
        val dog = opService.create().img(converted)

        // Do the DoG filtering using ImageJ Ops
        opService.filter().dog(dog, converted, 1.0, 1.25)

        // Create a NormalizeScaleRealTypes op
        val scale_op: RealTypeConverter<FloatType, T>
        scale_op = opService.op("convert.normalizeScale", dog.firstElement(), image.firstElement()) as RealTypeConverter<FloatType, T>

        // Create the output image
        output = opService.create().img(image)

        // Run the op to do type conversion for better displaying
        opService.convert().imageType(output, dog, scale_op)

        // You can also use the OpService to run the op
        // opService.run(Ops.Convert.ImageType.class, output, dog, scale_op);
    }
//
//    companion object {
//
//        /*
//     * This main method is for convenience - so you can run this command
//     * directly from Eclipse (or other IDE).
//     * <p>
//     * It will launch ImageJ and then run this command using the CommandService.
//     * This is equivalent to clicking "Tutorials&gt;DoG Filtering" in the UI.
//     * </p>
//     */
//        @Throws(Exception::class)
//        @JvmStatic
//        fun main(args: Array<String>) {
//            // Launch ImageJ as usual.
//            val ij = ImageJ()
//            ij.launch(*args)
//
//            // ask the user for a file to open
//            val file = ij.ui().chooseFile(null, "open")
//
//            if (file != null) {
//                // load the dataset
//                val dataset = ij.scifio().datasetIO().open(file.path)
//
//                // show the image
//                ij.ui().show(dataset)
//
//                // Launch the "UsingOpsDog" command.
//                ij.command().run(UsingOpsDog<*>::class.java, true)
//            }
//
//        }
//    }

}