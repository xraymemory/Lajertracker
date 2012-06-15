(ns laser.core
	(:use quil.core [quil.applet :only [current-applet applet-close]])
	(:require clojure.contrib.math)
	(:import codeanticode.gsvideo.GSCapture
			 codeanticode.gsvideo.GSLibraryLoader
			 codeanticode.gsvideo.GSLibraryPath
			 codeanticode.gsvideo.GSMovie
			 codeanticode.gsvideo.GSPipeline
			 codeanticode.gsvideo.GSPlayer
			 codeanticode.gsvideo.GSVideo
			 edu.bard.drab.PCLT.Homography
			 edu.bard.drab.PCLT.LaserPoint
			 edu.bard.drab.PCLT.PCLT 
			 processing.core.PGraphics 
			 processing.core.PGraphics2D
			 processing.core.PGraphics3D
			 ))

(defn setup []
	(smooth)
	(background 20)
	(color-mode :rgb)
	(def last-image)
	(def c (color 255 255 255))
	(def menu-size 75)
	(def mode 0)
	(def options 0)
	(def PEN 1)
	(def LINE 2)
	(def CIRCLE 3)
	(def RECT 4)
	(def CLEAR 5)
	(def sbc 270)
	(def offscreen (. (current-applet) g))
	(def drawscreen (GSCapture. (current-applet) 640 480 "dev/video0"))
	(def lt (PCLT. (current-applet) nil [^GSCapture drawscreen]))
	(. offscreen stroke 0)
	(. offscreen fill 0)
	(. offscreen smooth)
	(. offscreen color-mode :HSB)
	(. offscreen fill 255)
	(. offscreen background 0)
	(. offscreen rect-mode :CENTER))

; when a laser is found, this function checks where it is 
(defn laser-pressed [p] 
	; laser is in the menu
	(when (and (< (. p x) (menu-size)) (< (. p y ) (- (. (current-applet) height) (/ menu-size 2))))
		(def omode (/ (- (. p y) (/ menu-size 2)) menu-size))
		(if (== omode CLEAR) (. offscreen background 0)  (def mode omode)))
	; laser is on the color bar
	(if (>= (. p y) (- height menu-size)) 
		(def c (. p x)))
	; laser is on the stroke weight bar
	(when (and (>= (. p y) menu-size) (<= (. p y) (- height menu-size))) 
		(def sbc (. p y))))

(defn draw-menu [] 
	(. offscreen stroke-weight 2)
	(. offscreen fill 24)
	(. offscreen stroke 24)
	(. offscreen rect (/ menu-size 2) (+ (/ menu-size 2) (/ (* menu-size options) 2)) menu-size (* menu-size options))
	
	(dotimes [i options] (
		(def x (+ (/ menu-size 2) 3))
		(def y (+ (* i menu-size) menu-size)))
		(. offscreen stroke 255)
		(if (== i mode) 
			(. offscreen fill (map-range c 0 (. (current-applet) width) 0 255) 255 255 128)
			(. offscreen no-fill))
		(. offscreen rect x y menu-size menu-size)
		(. offscreen no-fill)
		(when (== i PEN) 
			(. offscreen line (+ x (/ menu-size 3)) (- y (/ menu-size 3)) (- x (/ menu-size 5)) (- y (/ menu-size 5)))
			(. offscreen line x y (+ x (/ menu-size 5)) (+ y (/ menu-size 5)))
			(. offscreen line x y (+ x (/ menu-size 3)) (- y (/ menu-size 3))))
		(when (== i LINE)
			(. offscreen no-fill)
			(. offscreen line (- x (/ menu-size 3)) (+ y (/ menu-size 3)) (+ x (/ menu-size 3)) (- y (/ menu-size 3))))
		(when (== i CIRCLE)
			(. offscreen no-fill)
			(. offscreen ellipse x y (/ menu-size 2) (/ menu-size 2)))
		(when (== i RECT)
			(. offscreen no-fill)
			(. offscreen rect x y (/ menu-size 2) (/ menu-size 2)))	
		))
	

(defn draw-color-bar [] 
	(. offscreen no-stroke)
	(. offscreen fill 0)
	(. ofscreen rect 0 (- (. (current-applet) height) (/ menu-size 2)) (. (current-applet) width) (/ menu-size 2))
	(dotimes [i 255] (
		(. offscreen fill i 255 255)
		(let [x (map-range i 0 255 0 width)])
		(. offscreen rect x (- height (/ menu-size 2)) (+ (/ width 255) 1) (/ menu-size 2))))
	)

(defn draw-weight-bar [] 
	(push-matrix)
	(translate (- width menu-size) 0)
	(. offscreen fill 0)
	(. offscreen rect-mode :CORNERS)
	(. offscreen rect ((- menu-size) 0 menu-size height))
	(. offscreen stroke-weight 3)
	(. offscreen stroke 255)
	(. offscreen no-fill)
	(. offscreen begin-shape)
	(. offscreen vertex 0 menu-size)
	(. offscreen vertex 40 menu-size)
	(. offscreen vertex 24 (- height menu-size))
	(. offscreen vertex 16 (- height menu-size))
	(. offscreen vertex 0 menu-size)
	(. offscreen end-shape)
	(. offscreen rect-mode :CENTER)
	(. ofscreen rect ((/ menu-size 4) sbc menu-size (/ menu-size 4)))
	(pop-matrix)
	)

(defn draw[] 
	(draw-menu)
	(draw-color-bar)
	(draw-weight-bar)
	(. offscreen (map-range c 0 (. (current-applet) width) 0 255) 255 255)
	(. offscreen no-fill)
	(. offscreen stroke-weight (map-range sbc menu-size (- (. current-applet height) menu-size) 25 1))
	
	(dotimes [i (. lt numOfPoints)]
		(def p (aget (. lt points) i))
		(when (and (> (. p duration) 50) (> (. p x) menu-size) (< (. p y) (- (. current-applet height) (/ menu-size 2))))
			(if (== mode PEN) (. offscreen line (. p px) (. p py) (. p x) (. p y)))
			(when (< (. p duration 300))
				(def centerx (. p x))
				(def centery (. p y))
				(def last-image (get 0 0 (. (current-applet) width) (. (current-applet) height))))
			(if (!= last-image nil) (. offscreen image last-image 0 0 (. last-image width) (. last-image height)))
			(if (== mode LINE) (. offscreen line centerx centery (. p x) (. p y)))
			(if (== mode CIRCLE) (. offscreen ellipse centerx centery (* 2 (abs (- (. p x) centerx))) (* 2 (abs (- (. p y) centery)))))
			(if (== mode RECT) (. offscreen rect centerx centery (* 2 (abs (- (. p x) centerx))) (* 2 (abs (- (. p y) centery)))))
			)
	)
)

(defn key-pressed []
	(if (== key ' ') (. offscreen background 0))
)

(defsketch laser
	:title "Laser tracking example"
	:setup setup
	:draw draw
	:size[640 480]
	:key-pressed key-pressed
	)

