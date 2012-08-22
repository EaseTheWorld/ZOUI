Android ZO UI
===========================

Have you ever felt that 'multiple click' needs too much energy to fit in the button?
Well, I have. Imagine that you see the button, click once and close your eyes, click ten times.
You can't be absolutely sure you correctly clicked ten times while you are not looking at it.
There's also finger muscle issue. I feel drag (stroke) is more comfortable than click (up-stop-down). 

'ZO UI' is a stroke-based ui. Its name is from its two modes, 'Z' and 'O'.

Mode Z
------
Mode Z is one trigger per one stroke.
- If you stroke(or just fling) the button in any direction. Then `onMove(Z, 1)` will be called once. It is like one click.
- If you stroke go and back without touch up. Then `onMove(Z, 1)` will be called twice. It is like two click.
- If you stroke go and back and go without touch up. Then `onMove(Z, 1)` will be called three times. It is like three click.
  (You are drawing 'Z' shape. That's why this is mode 'Z'.) Now you get it. `onMove(Z, 1)` is simply called when you stroke.
- One day, I wanted to make '+', '-' buttons in one. So I split 'any' direction to 'north' and 'south' directions.
  If direction of the first stroke is 'north', `onMove(Z, -1)` will be called until the touch up. Which means direction of subsequent strokes doesn't matter.

Mode O
------
Mode Z is for few-time trigger but I need something for more-than-10-time trigger.
- Mode O is like invisible progress bar. You stroke any direction then `onMove(O, value)` will be called while you are moving.
- To reverse the sign of the value, move backward. First sign is always positive.
- The size of the value is in proportion to the moving distance.
- Distance threshold can be set in `ZOTouchListener(context, listener, distanceThresholdDip)`. It is in dip unit so it will be scaled automatically.

Mode Z -> O
----------
- Default mode is mode Z.
- To use mode O, touch long before stroke. Long touch at first internally change to mode O.
- If you want to disable this and fix the mode, use `ZOTouchListener.setMode()`.

Additional dev module
---------------------
- `StrokeTracker` : Internal module to detect angle between strokes and handle stroke states. Used by `StrokeGestureDetector`.
- `StrokeGestureDetector` : Internal module to feed MotionEvent to `StrokeTracker`. Used by `ZOTouchListener`.
- `ZOTouchListener` : Handle mode Z and O. You will use this.
- `RecentBuffer` : Simple buffer to contain latest n items and remove oldest automatically. It will make only n items and recycle after that.
- `TouchPaintView` : This is like TouchPaint in ApiDemos with drag-move and pinch-zoom.
- `GestureAnalyzer` : My test tool to analyze strokes. This is useful for any single-touch touch event analysis.

Release Notes
-------------
- v0.1.0 : Initial Release

Source
------
https://github.com/EaseTheWorld/ZOUI

Made by **EaseTheWorld**