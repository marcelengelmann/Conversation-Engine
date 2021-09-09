package de.dai_labor.conversation_engine_gui.gui_components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.inject.Singleton;

import de.dai_labor.conversation_engine_core.interfaces.ISkill;
import de.dai_labor.conversation_engine_gui.models.DragElementData;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * The DialoguePane displays all the {@link ISkill skill}'s {@link State states}
 * and {@link Transition transitions}
 *
 * @author Marcel Engelmann
 *
 */
@Singleton
public class DialoguePane extends Pane {

	private DragElementData dragElementData;
	private Pane dialogueModelDataLayer;
	private static final double MIN_ZOOM_LEVEL = 0.2;
	private static final double MAX_ZOOM_LEVEL = 3.0;
	private BiConsumer<Double, Double> addState;
	private BiConsumer<State, State> addTransition;
	private Consumer<State> removeState;
	private Consumer<Transition> removeTransition;
	private SimpleStringProperty insertMode;
	private State sourceTransitionState;
	private State targetTransitionState;
	private SimpleObjectProperty<State> selectedState;
	private SimpleObjectProperty<Transition> selectedTransition;
	private TransitionArrow dragArrow;
	private boolean mouseIsPressed = false;

	/**
	 * EventHandler that filters all mouse events
	 */
	private EventHandler<MouseEvent> mouseEventFilter = event -> {
		// if any toggleButton is active -> do not allow the State children to fire the
		// event. Instead redirect the event to this Pane
		if (this.insertMode.getValue().equals("addState") && this.isStateTarget(event)) {
			event.consume();
			Event.fireEvent(this, event);
		}
	};

	/**
	 * EventHandler that handles the key pressed event. It deletes the selected
	 * {@link State} or {@link Transition} on pressing the delete key
	 */
	private EventHandler<KeyEvent> keyEventFilter = event -> {
		if (event.getCode() == KeyCode.DELETE) {
			if (this.selectedState.get() != null) {
				this.removeState.accept(this.selectedState.get());
				this.selectedState.set(null);
			}
			if (this.selectedTransition.get() != null) {
				this.removeTransition.accept(this.selectedTransition.get());
				this.selectedTransition.set(null);
			}
		}
	};

	/**
	 * EventHandler that handles the mouse pressed event. It stores the
	 * {@link DragElementData} for the {@link #mouseDraggedEventFilter dragging
	 * event} or stores the source {@link State} of a {@link Transition} if the
	 * addTransition toggle button is active
	 */
	private EventHandler<MouseEvent> mousePressedEventFilter = event -> {
		this.mouseIsPressed = true;
		this.requestFocus();
		this.deselectAll();
		if (event.isSecondaryButtonDown()) {
			this.setCursor(Cursor.MOVE);
			this.dragElementData = new DragElementData(event.getScreenX(), event.getScreenY(),
					this.dialogueModelDataLayer.getTranslateX(), this.dialogueModelDataLayer.getTranslateY());
			event.consume();
		} else if (this.insertMode.get().equals("addTransition") && this.isStateTarget(event)) {
			this.sourceTransitionState = (State) event.getPickResult().getIntersectedNode();
			event.consume();
		} else if (!this.insertMode.get().isBlank()) {
			event.consume();
		}

	};

	/**
	 * EventHandler that handles the mouse dragged event. It will move the
	 * {@link #dialogueModelDataLayer} if the secondary mouse button is pressed
	 * while dragging. If the addTransition toggle button is active, it will create
	 * a temporally {@link TransitionArrow} from the {@link #sourceTransitionState
	 * source state} to the mouse pointer
	 */
	private EventHandler<MouseEvent> mouseDraggedEventFilter = event -> {
		if (event.isSecondaryButtonDown()) {
			double scale = 1.0;
			double xDifference = (event.getScreenX() - this.dragElementData.getMouseX()) / scale
					+ this.dragElementData.getTranslateX();
			double yDifference = (event.getScreenY() - this.dragElementData.getMouseY()) / scale
					+ this.dragElementData.getTranslateY();
			this.dialogueModelDataLayer.setTranslateX(xDifference);
			this.dialogueModelDataLayer.setTranslateY(yDifference);

			event.consume();
		} else if (this.insertMode.get().equals("addTransition")) {
			if (this.sourceTransitionState != null) {
				this.dialogueModelDataLayer.getChildren().remove(this.dragArrow);
				if (event.getPickResult().getIntersectedNode() == this.sourceTransitionState) {
					this.dragArrow = new TransitionArrow(this.sourceTransitionState, this.sourceTransitionState,
							this.dialogueModelDataLayer.getScaleX());
				} else {
					Double scale = this.dialogueModelDataLayer.getScaleX();
					StackPane tmpPane = new StackPane();
					tmpPane.setTranslateX(event.getX() - this.dialogueModelDataLayer.getBoundsInParent().getMinX());
					tmpPane.setTranslateY(event.getY() - this.dialogueModelDataLayer.getBoundsInParent().getMinY());
					this.dragArrow = new TransitionArrow(this.sourceTransitionState, tmpPane, scale);
				}
				this.dragArrow.setMouseTransparent(true);
				this.dialogueModelDataLayer.getChildren().add(this.dragArrow);
			}
			event.consume();
		}
	};

	/**
	 * EventHandler that handles the mouse released event. It resets the cursor when
	 * the secondary mouse button was released. If the primary mouse button was
	 * released, then it will create a new State or transition if one of the toggle
	 * buttons were active
	 */
	private EventHandler<MouseEvent> mouseReleasedEventFilter = event -> {
		this.dialogueModelDataLayer.getChildren().remove(this.dragArrow);
		if (event.getButton() == MouseButton.SECONDARY) {
			this.setCursor(Cursor.DEFAULT);
			event.consume();
		} else if (event.getButton() == MouseButton.PRIMARY) {
			if (this.insertMode.get().equals("addState")) {
				Double scale = this.dialogueModelDataLayer.getScaleX();
				double x = (event.getX() - this.dialogueModelDataLayer.getBoundsInParent().getMinX()) / scale;
				double y = (event.getY() - this.dialogueModelDataLayer.getBoundsInParent().getMinY()) / scale;
				this.addState.accept(x, y);
				event.consume();
			} else if (this.insertMode.get().equals("addTransition") && this.isStateTarget(event)) {
				this.targetTransitionState = (State) event.getPickResult().getIntersectedNode();
				this.addTransition.accept(this.sourceTransitionState, this.targetTransitionState);
				this.sourceTransitionState = null;
				this.targetTransitionState = null;
				event.consume();
			}
		}
		this.mouseIsPressed = false;
	};

	/**
	 * EventHandler that handles the mouse scroll event. It zooms the
	 * {@link #dialogueModelDataLayer} in or out.
	 *
	 * This method was partly taken from this <a href=
	 * "https://stackoverflow.com/questions/29506156/javafx-8-zooming-relative-to-mouse-pointer/29530135#29530135">source</a>
	 */
	private EventHandler<ScrollEvent> mouseScrollEventFilter = event -> {
		if (this.mouseIsPressed) {
			return;
		}
		double zoomFactor = 1.2;

		double scale = this.dialogueModelDataLayer.scaleXProperty().get();
		double oldScale = scale;

		if (event.getDeltaY() < 0) {
			scale /= zoomFactor;
		} else {
			scale *= zoomFactor;
		}

		scale = clamp(scale, MIN_ZOOM_LEVEL, MAX_ZOOM_LEVEL);

		double f = scale / oldScale - 1;

		double dx = event.getX() - (this.dialogueModelDataLayer.getBoundsInParent().getWidth() / 2
				+ this.dialogueModelDataLayer.getBoundsInParent().getMinX());
		double dy = event.getY() - (this.dialogueModelDataLayer.getBoundsInParent().getHeight() / 2
				+ this.dialogueModelDataLayer.getBoundsInParent().getMinY());

		this.dialogueModelDataLayer.setScaleX(scale);
		this.dialogueModelDataLayer.setScaleY(scale);

		// note: pivot value must be untransformed, i. e. without scaling
		this.setPivot(f * dx, f * dy);

		// resize to fit parent
		event.consume();
	};

	/**
	 * Creates a new DialoguePane
	 *
	 * @param dialogueModelDataLayer The {@link Pane} that handles all displayed
	 *                               elements like {@link State States} and
	 *                               {@link Transition Transitions}
	 * @param insertMode             The Property that stores the current insert
	 *                               mode. The mode is determined by the current
	 *                               active toggle button
	 * @param selectedState          The Property that holds the currently selected
	 *                               {@link State}
	 * @param selectedTransition     The Property that holds the currently selected
	 *                               {@link Transition}
	 * @param addState               The Method that is called to create a new
	 *                               {@link State}
	 * @param addTransition          The Method that is called to create a new
	 *                               {@link Transition}
	 * @param removeState            The Method that is called to remove a
	 *                               {@link State}
	 * @param removeTransition       The Method that is called to remove a
	 *                               {@link Transition}
	 */
	public DialoguePane(Pane dialogueModelDataLayer, SimpleStringProperty insertMode,
			SimpleObjectProperty<State> selectedState, SimpleObjectProperty<Transition> selectedTransition,
			BiConsumer<Double, Double> addState, BiConsumer<State, State> addTransition, Consumer<State> removeState,
			Consumer<Transition> removeTransition) {
		this.dialogueModelDataLayer = dialogueModelDataLayer;
		this.addState = addState;
		this.addTransition = addTransition;
		this.removeState = removeState;
		this.removeTransition = removeTransition;
		this.insertMode = insertMode;
		this.selectedState = selectedState;
		this.selectedTransition = selectedTransition;
		this.getChildren().add(0, this.dialogueModelDataLayer);
		this.addEventFilter(MouseEvent.MOUSE_PRESSED, this.mousePressedEventFilter);
		this.addEventFilter(MouseEvent.MOUSE_DRAGGED, this.mouseDraggedEventFilter);
		this.addEventFilter(MouseEvent.MOUSE_RELEASED, this.mouseReleasedEventFilter);
		this.addEventFilter(MouseEvent.ANY, this.mouseEventFilter);
		this.addEventFilter(ScrollEvent.ANY, this.mouseScrollEventFilter);
		this.addEventFilter(KeyEvent.ANY, this.keyEventFilter);
	}

	/**
	 * Deselects the currently selected {@link State} and {@link Transition}
	 */
	public void deselectAll() {
		this.selectedState.set(null);
		this.selectedTransition.set(null);
	}

	/**
	 * Centers the element of the {@link DialoguePane} by calculating the center of
	 * all element's
	 */
	public void center() {
		double targetX = 0.0;
		double targetY = 0.0;
		this.dialogueModelDataLayer.setScaleX(1.0);
		this.dialogueModelDataLayer.setScaleY(1.0);
		this.dialogueModelDataLayer.setTranslateX(0);
		this.dialogueModelDataLayer.setTranslateY(0);
		this.dialogueModelDataLayer.setLayoutX(0);
		this.dialogueModelDataLayer.setLayoutY(0);
		List<State> allStates = this.getAllStates();
		for (State state : allStates) {
			Bounds bounds = state.getBoundsInParent();
			targetX += bounds.getCenterX();
			targetY += bounds.getCenterY();
		}
		targetX /= allStates.size();
		targetY /= allStates.size();
		targetX = this.getWidth() / 2 + this.getScene().getWidth() / 2 - targetX;
		targetY = this.getHeight() / 2 + this.getScene().getHeight() / 2 - targetY;
		this.dialogueModelDataLayer.relocate(targetX, targetY);
	}

	/**
	 * Gets a list of all {@link State States} of the
	 * {@link #dialogueModelDataLayer}
	 *
	 * @return a list of all {@link State States} of the
	 *         {@link #dialogueModelDataLayer}
	 */
	private List<State> getAllStates() {
		List<State> allStates = new ArrayList<>();
		for (Node element : this.dialogueModelDataLayer.getChildren()) {
			if (element instanceof State) {
				allStates.add((State) element);
			}
		}
		return allStates;
	}

	/**
	 * Sets the pivot for the scroll event
	 *
	 * <a href=
	 * "https://stackoverflow.com/questions/29506156/javafx-8-zooming-relative-to-mouse-pointer/29530135#29530135">SOURCE</a>
	 *
	 * @param x The x value to be set
	 * @param y The y value to be set
	 */
	private void setPivot(double x, double y) {
		this.dialogueModelDataLayer.setTranslateX(this.dialogueModelDataLayer.getTranslateX() - x);
		this.dialogueModelDataLayer.setTranslateY(this.dialogueModelDataLayer.getTranslateY() - y);
	}

	/**
	 * Checks whether the given value is between the given bounds and returns the
	 * value or the nearest bound <a href=
	 * "https://stackoverflow.com/questions/29506156/javafx-8-zooming-relative-to-mouse-pointer/29530135#29530135">SOURCE</a>
	 *
	 * @param value The value to check
	 * @param min   The minimum bound
	 * @param max   The minimum bound
	 * @return The value or the nearest bound, if the value is outside of the bounds
	 */
	private static double clamp(double value, double min, double max) {

		if (Double.compare(value, min) < 0) {
			return min;
		}

		if (Double.compare(value, max) > 0) {
			return max;
		}

		return value;
	}

	/**
	 * Checks if the target of an event is a {@link State}
	 *
	 * @param event The event to check the target of
	 * @return true if the target of an event is a {@link State}
	 */
	private boolean isStateTarget(MouseEvent event) {
		Class<? extends Node> eventTargetClass = event.getPickResult().getIntersectedNode().getClass();
		return eventTargetClass.equals(State.class);
	}

}