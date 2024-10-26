package com.buaisociety.pacman.entity.behavior;

//og imports 
import com.buaisociety.pacman.entity.Direction;
import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.PacmanEntity;
import com.cjcrafter.neat.compute.Calculator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.buaisociety.pacman.maze.Maze;
import com.buaisociety.pacman.maze.Tile;
import com.buaisociety.pacman.maze.TileState;
import com.buaisociety.pacman.sprite.DebugDrawing;
import com.cjcrafter.neat.Client;
import com.buaisociety.pacman.entity.Direction;
import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.PacmanEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import java.util.LinkedList;
import java.util.*;
import com.buaisociety.pacman.Searcher;
import com.buaisociety.pacman.maze.Tile;
import com.cjcrafter.neat.compute.Calculator;
// import com.buaisociety.pacman.entity.behavior.NeatPacmanBehavior;
public class TournamentBehavior implements Behavior {

    private final Calculator calculator;
    private @Nullable PacmanEntity pacman;

    private int previousScore = 0;
    private int framesSinceScoreUpdate = 0;

    public TournamentBehavior(Calculator calculator) {
        this.calculator = calculator;
    }

    /**
     * Returns the desired direction that the entity should move towards.
     *
     * @param entity the entity to get the direction for
     * @return the desired direction for the entity
     */
    @NotNull
    @Override
    public Direction getDirection(@NotNull Entity entity) {
        // --- DO NOT REMOVE ---
        if (pacman == null) {
            pacman = (PacmanEntity) entity;
        }

        int newScore = pacman.getMaze().getLevelManager().getScore();
        if (previousScore != newScore) {
            previousScore = newScore;
            framesSinceScoreUpdate = 0;
        } else {
            framesSinceScoreUpdate++;
        }

        if (framesSinceScoreUpdate > 60 * 40) {
            pacman.kill();
            framesSinceScoreUpdate = 0;
        }
        // --- END OF DO NOT REMOVE ---

        // TODO: Put all your code for info into the neural network here


       //not NORMALIZING
       //float distanceToNearestPellet = minDistance;
       //float maxY = dimensions.y();
       //float maxX = dimensions.x();
       //float maxDistance = (float) Math.sqrt(Math.pow(maxX, 2) + Math.pow(maxY, 2));


       //NORMALIZING
       //float normalizedPelletX = nearestPelletX / dimensions.x();
       //float normalizedPelletY = nearestPelletY / dimensions.y();
       //float normalizedDistToNearestPellet = distanceToNearestPellet / maxDistance;


       // END OF SPECIAL TRAINING CONDITIONS


       // We are going to use these directions a lot for different inputs. Get them all once for clarity and brevity
       Direction forward = pacman.getDirection();
       Direction left = pacman.getDirection().left();
       Direction right = pacman.getDirection().right();
       Direction behind = pacman.getDirection().behind();


       // Input nodes 1, 2, 3, and 4 show if the pacman can move in the forward, left, right, and behind directions
       boolean canMoveForward = pacman.canMove(forward);
       boolean canMoveLeft = pacman.canMove(left);
       boolean canMoveRight = pacman.canMove(right);
       boolean canMoveBehind = pacman.canMove(behind);


       // input nodes show if the nearest pellet is forward, left, right, or behind
       //boolean pelletIsForward = nearestPelletY < pacmanY;
       //boolean pelletIsLeft = nearestPelletX < pacmanX;
       //boolean pelletIsRight = nearestPelletX > pacmanX;
       //boolean pelletIsBehind = nearestPelletY > pacmanY;


       Tile currentTile = pacman.getMaze().getTile(pacman.getTilePosition());
       Map<Direction, Searcher.SearchResult> nearestPellets = Searcher.findTileInAllDirections(currentTile, tile -> tile.getState() == TileState.PELLET);


       int maxDistance = -1;
       for (Searcher.SearchResult result : nearestPellets.values()) {
           if (result != null) {
               maxDistance = Math.max(maxDistance, result.getDistance());
           }
       }


       float nearestPelletForward = nearestPellets.get(forward) != null ? (float)0.5 + (1 - ((float) nearestPellets.get(forward).getDistance() / maxDistance)) * 0.5f : (float)0.5;
       float nearestPelletLeft = nearestPellets.get(left) != null ? (float)0.5 + (1 - ((float) nearestPellets.get(left).getDistance() / maxDistance)) * 0.5f : (float)0.5;
       float nearestPelletRight = nearestPellets.get(right) != null ? (float)0.5 + (1 - ((float) nearestPellets.get(right).getDistance() / maxDistance)) * 0.5f : (float)0.5;
       float nearestPelletBehind = nearestPellets.get(behind) != null ? (float)0.5 + (1 - ((float) nearestPellets.get(behind).getDistance() / maxDistance)) * 0.5f : (float)0.5;


        float[] inputs = new float[] {
            //pelletIsForward ? 1f : 0f,
           //pelletIsLeft ? 1f : 0f,
           //normalizedDistToNearestPellet,
           //normalizedPelletX,
           //normalizedPelletY,
           canMoveForward ? 1f : 0f,
           canMoveLeft ? 1f : 0f,
           canMoveRight ? 1f : 0f,
           canMoveBehind ? 1f : 0f,
           nearestPelletForward,
           nearestPelletLeft,
           nearestPelletRight,
           nearestPelletBehind
        };
        float[] outputs = calculator.calculate(inputs).join();

        // Chooses the maximum output as the direction to go... feel free to change this ofc!
        // Adjust this to whatever you used in the NeatPacmanBehavior.class
        int index = 0;
        float max = outputs[0];
        for (int i = 1; i < outputs.length; i++) {
            if (outputs[i] > max) {
                max = outputs[i];
                index = i;
            }
        }

        return switch (index) {
            case 0 -> pacman.getDirection();
            case 1 -> pacman.getDirection().left();
            case 2 -> pacman.getDirection().right();
            case 3 -> pacman.getDirection().behind();
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
    }
}
