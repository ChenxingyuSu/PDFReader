Chenxingyu Su
20755516 c35su
openjdk version "11.0.8" 2020-07-14
Android SDK 30
Windows 10, intellij gradle Lenovo YOGA 730 13"

Source of icons:
Pen icon: from the android icon library, "create"
Eraser icon: https://www.flaticon.com/free-icons/eraser

The current page number and the total number of pages are showing with the title of the PDF file.
The pen icon is in annotation mode by default, however by click on it, it switches to a highlighter with a toast indicating the switch at bottom.
Similarly, if it is currently in highlighter mode, then by clicking the icon again will switch back to the annotation mode.

Undo/redo can be found in the action overflow button.

Features incompleted:
Erase an annotation using the erase tool.
Regarding erase, tried the suggested erase tool using region.op, with method checkInteraction(). 
However, cannot get it working properly.

Zoom and pan using standing gestures. Annotations scale with the canvas.
