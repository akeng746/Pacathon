package com.buaisociety.pacman.entity.behavior;


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
public class NeatPacmanBehavior implements Behavior {


   private final @NotNull Client client;
   private @Nullable PacmanEntity pacman;


   // Score modifiers help us maintain "multiple pools" of points.
   // This is great for training, because we can take away points from
   // specific pools of points instead of subtracting from all.
   private int scoreModifier = 0;
   private int explorationReward = 0;


   // new variables!! :)
   private int lastScore = 0;
   private int updatesSinceLastScore = 0;
   private int survivalDurationScore = 0;
   float nearestPelletX = 0;
   float nearestPelletY = 0;
   private final int maxHistorySize = 10;  // Limit of recent positions to store
   private final LinkedList<Vector2ic> recentPositions = new LinkedList<>();  // Store recent positions
   private final Set<Vector2ic> visitedTiles = new HashSet<>();




   public NeatPacmanBehavior(@NotNull Client client) {
       this.client = client;
   }


   /**
    * Updates position tracking (FOR LAST TEN MOVES)
    * @param currentPosition is the current pacman position
    */
   public void updatePositionTracking(@NotNull Vector2ic currentPosition) {
       // Check if the current position exists in the recent positions list
       if (!(recentPositions.contains(currentPosition))) {
           // Apply a penalty for revisiting the same position
           scoreModifier = 10;  // Adjust penalty value as needed
       }
       // Add the current position to the recent positions list
       recentPositions.add(currentPosition);
       // Maintain the max history size by removing the oldest position if necessary
       if (recentPositions.size() > maxHistorySize) {
           recentPositions.removeFirst();
       }
   }


   /**
    * Updates position tracking (FOR ALL MOVES)
    * @param currentPosition
    */
   public void rewardNewTileExploration(Vector2ic currentPosition) {
       if (!visitedTiles.contains(currentPosition)) {
           // Apply a reward for reaching a new tile
           explorationReward = 10;
           visitedTiles.add(new Vector2i(currentPosition));
       }
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
       if (pacman == null) {
           pacman = (PacmanEntity) entity;
       }


       // SPECIAL TRAINING CONDITIONS
       int newScore = pacman.getMaze().getLevelManager().getScore();
       if (newScore > lastScore) {
           lastScore = newScore;
           updatesSinceLastScore = 0;
       }
       if (updatesSinceLastScore++ > 60 * 10) {
           pacman.kill();
           return Direction.UP;
       }


       if (pacman.isAlive()) {
           survivalDurationScore++;
       } else {
           survivalDurationScore = 0;
       }
       Maze maze = pacman.getMaze();
       float pacmanX = pacman.getTilePosition().x();
       float pacmanY = pacman.getTilePosition().y();
       float minDistance = Float.MAX_VALUE;
       Vector2ic dimensions = pacman.getMaze().getDimensions();
       for (int y = 0; y < dimensions.y(); y++) {
           for (int x = 0; x < dimensions.x(); x++) {
               Tile tile = pacman.getMaze().getTile(x, y);
               if (tile.getState() == TileState.PELLET) {
                   float distance = (float) Math.sqrt(Math.pow(pacmanX - x, 2)
                       + Math.pow(pacmanY - y, 2));
                   if (distance < minDistance) {
                       nearestPelletX = x;
                       nearestPelletY = y;
                       minDistance = distance;
                   }
               }
           }
       }


       updatePositionTracking(pacman.getTilePosition());
       rewardNewTileExploration(pacman.getTilePosition());


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


       float[] outputs = client.getCalculator().calculate(new float[]{
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
       }).join();


       int index = 0;
       float max = outputs[0];
       for (int i = 1; i < outputs.length; i++) {
           if (outputs[i] > max) {
               max = outputs[i];
               index = i;
           }
       }


       Direction newDirection = switch (index) {
           case 0 -> pacman.getDirection();
           case 1 -> pacman.getDirection().left();
           case 2 -> pacman.getDirection().right();
           case 3 -> pacman.getDirection().behind();
           default -> throw new IllegalStateException("Unexpected value: " + index);
       };


       client.setScore((pacman.getMaze().getLevelManager().getScore() * 4) + scoreModifier + explorationReward +
           (survivalDurationScore * 0.2));
       return newDirection;
   }


   @Override
   public void render(@NotNull SpriteBatch batch) {
       // TODO: You can render debug information here
       /*
       if (pacman != null) {
           DebugDrawing.outlineTile(batch, pacman.getMaze().getTile(pacman.getTilePosition()), Color.RED);
           DebugDrawing.drawDirection(batch, pacman.getTilePosition().x() * Maze.TILE_SIZE, pacman.getTilePosition().y() * Maze.TILE_SIZE, pacman.getDirection(), Color.RED);
       }
        */
   }
}
