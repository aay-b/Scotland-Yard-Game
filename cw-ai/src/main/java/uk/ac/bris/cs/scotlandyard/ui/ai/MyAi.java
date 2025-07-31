package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class

MyAi implements Ai {

    private static final int MAX_DEPTH = 7;
    private static ImmutableSet<Piece> players;
    private final Set<Integer> alreadyVisitedLocations = new HashSet<>();
    private int[][] shortestPaths;
    // 2d array means each element is itself an array
    private Set<Integer> occupiedLocations;

    @Nonnull
    @Override
    public String name() {
        return "Sherbot Holmes";
    }


    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        players = board.getPlayers();
        occupiedLocations = new HashSet<>();
        for (Piece piece : players) {
            if (piece.isDetective()) {
                board.getDetectiveLocation((Piece.Detective) piece).ifPresent(occupiedLocations::add);
//                returns an optional value so if a value is present in that optional add it
            }
        }


        precomputeShortestPaths(board.getSetup());

        List<Move> moves = new ArrayList<>(board.getAvailableMoves());

        // MrX moves are handled here below
        moves.sort(Comparator.comparingDouble(move -> -(evaluateBoard(board, move.source()) + evaluateMove(board, move))));
        List<Piece> pieces = new ArrayList<>(players);
        pieces.removeIf(Piece::isMrX);
        //You're running minimax to simulate what the detectives will do
        //→ So the recursion should only include detectives, not MrX.

//      Now for each move:
//        Evaluate each of MrX’s possible moves
//
//        Simulate each move
//
//        Run minimax to see how detectives might respond
//
//        Pick the move with the highest total score


//        simulateMrXMove(pieces, board)
//        Builds a new simulated board where MrX has made the move

//        Pair.pair(...) is a factory method used to create a two-element container
//        minimax + evaluateMove = scoring function
//        descending list of best moves is being passed here and then for each move do the
//        following. Descending why? so that you hit the best guesses early, improving the
//        chances of early pruning during minimax

//        so each thread returns a pair of move and a value that is the score? and then max
//        collects all these pairs
//
//        first sorting was done on a quick, shallow evaluation:
//        this below is the real thing

//        each move inside moves is store in 'move'

//        Go through every possible MrX move, simulate it, run minimax to see
//        how good it is, and pick the best one.

//        .map(f) = For every element in the stream, apply function f, and return a
//        new stream with the results


//        A thread in Java is a lightweight unit of execution. It allows a program to perform
//        multiple tasks simultaneously by executing different parts of code concurrently.
//
//        Java supports multithreading, which means multiple threads can run in parallel, sharing
//        the same process memory but executing independently.

        Move bestMove = moves.parallelStream()
                //map transforms each move into a Pair of left and right
                .map(move -> Pair.pair(move, miniMax(simulateMrXMove(pieces, board), getDestination(move), MAX_DEPTH - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, pieces) + evaluateMove(board, move)))
                .max(Comparator.comparingDouble(pair -> pair.right()))
                //here map extracts the move for that max value above
                .map(pair -> pair.left()).orElse(moves.get(0));

        updateVisitedLocations(bestMove);
        return bestMove;
    }

//    minimax -> “If I do this, what will THEY do? Then what can I do after that?”
//    It looks several moves ahead and returns a realistic prediction of how good a move is.

    private double miniMax(Board board, int mrXLocation, int depth, double alpha, double beta, List<Piece> remaining) {
        if (depth == 0 || !board.getWinner().isEmpty()) {
            return evaluateBoard(board, mrXLocation);
//            You return a numeric score for how good the current position is
//            from MrX’s perspective
//            Higher = better
//            Lower = worse
//            if you hit the limit or end of the game → just return a score,
        }

        Piece currentPiece;
        List<Piece> nextRemaining;

        if (remaining.isEmpty()) {
            currentPiece = players.stream().filter(Piece::isMrX).findFirst().get();
            nextRemaining = players.stream().filter(piece -> !piece.isMrX()).collect(Collectors.toList());
        } else {
            currentPiece = remaining.get(0);
            nextRemaining = new ArrayList<>(remaining);
            nextRemaining.remove(0);
        }

//        I’m telling the priority queue to store Move objects, but to order them
//        based on a score calculated using evaluateBoard() and evaluateMove().
//        The custom comparator assigns a score to each move, and the queue uses
//        that score to keep the moves in descending order — best move at the top.
//        The moves themselves are stored, not the scores.

        PriorityQueue<Move> moves = new PriorityQueue<>(Comparator.comparingDouble(move -> -(evaluateBoard(board, mrXLocation) + evaluateMove(board, move))));
//        this creates an empty priority queue which will hold move objects and rank them
//        based on the score but rn it doesn't hold anything
        moves.addAll(board.getAvailableMoves());
        //now for every move we add the queue is filled
        if (currentPiece.isMrX()) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (Move candidateMove : moves) { //best looking moves first
                Board simulatedBoard = simulateMrXMove(nextRemaining, board);
                //creates a new simulated board to check what it would look like
                //if MrX made this move
                double eval = miniMax(simulatedBoard, getDestination(candidateMove), depth - 1, alpha, beta, nextRemaining) + evaluateMove(board, candidateMove);
                maxEval = Math.max(maxEval, eval); // best score for MrX
                alpha = Math.max(alpha, eval);  // if this alpha is better than previous update
                if (alpha >= beta) {
                    // if MrX's best option is already better than what the detectives
//                    would allow you can prune everything else
                    break;
                }
            }
            return maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            for (Move candidateMove : moves) {
                Board simulatedBoard = simulateDetectiveMove(currentPiece, nextRemaining, mrXLocation, board, candidateMove);
                double eval = miniMax(simulatedBoard, mrXLocation, depth - 1, alpha, beta, nextRemaining);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
//        so its like for each MrX move it will look forward 7 moves then tell MrX
//        to make a move and then do same thing again for next MrX move
    }

    //A generic type is a class, interface, or method that uses type parameters instead
    // of concrete types
//    e.g.
//    class Box<T> {
//    private T item;
//    public void set(T item) { this.item = item; }
//    public T get() { return item; }
//}
//    and then I can do:
//      Box<String> nameBox = new Box<>();
//      Box<Integer> numberBox = new Box<>();

    //generics are helpful because they catch ur code at compile time rather than runtime


    private int getDestination(Move move) {
        return move.accept(new Move.Visitor<>() {
//            Double dispatch means two dynamic decisions are made — one on the object
//            being visited, and one on the method inside the visitor.

//            In my AI, when I do move.accept(visitor), the move chooses the correct
//            visit() method based on its type (SingleMove or DoubleMove) at runtime.
//            That’s where double dispatch happens.\\

//            The dispatch to visitor.visit(this) is hidden inside the accept() method of
//            Move, but it’s still happening — that’s what makes it double dispatch.



//            so basically its double dispatch because
//            1st dispatch is when move.accept(visitor) gets a type of move at runtime -
//            Double or Single
//            2nd dispatch is when visitor.visit(this) which is a hidden method is called and
//            tells the backend that look it was a double move so ur going to play a double move
            @Override
            public Integer visit(Move.SingleMove singleMove) {
                return singleMove.destination;
            }

            @Override
            public Integer visit(Move.DoubleMove doubleMove) {
                return doubleMove.destination2;
            }
        });
    }

    private void updateVisitedLocations(Move move) {
//        no return here so not inferred it will be void like it did with int above
        move.accept(new Move.Visitor<Void>() {
            @Override
            public Void visit(Move.SingleMove singleMove) {
                alreadyVisitedLocations.add(singleMove.destination);
                return null;
            }

            @Override
            public Void visit(Move.DoubleMove doubleMove) {
                alreadyVisitedLocations.add(doubleMove.destination1);
                alreadyVisitedLocations.add(doubleMove.destination2);
                return null;
            }
        });
    }

//    evaluateBoard: “Scores how good the board state is for MrX.
//    after move has been made”
//
//    evaluateMove: “Scores how good a specific move is for MrX.”
//
//    simulateDetectiveMove: “Fakes the board as if a detective made a move.”
//
//    simulateMrXMove: “Sets up the board for detectives to start moving after MrX’s turn.”

    private double evaluateBoard(Board board, int mrXLocation) {
        List<Piece> detectives = new ArrayList<>(players);
        detectives.removeIf(Piece::isMrX);
        Piece detective = detectives.get(0);
        if (board.getWinner().contains(detective)) {
            return Double.NEGATIVE_INFINITY;
        }

        // Calculate distance to the closest detective and unoccupied neighbours as freedom score
        double minDistance = getMinDistance(mrXLocation);
        double freedomScore = calculateFreedomScore(board, mrXLocation);

        return (minDistance * 30) + (freedomScore * 10);
    }

    private double evaluateMove(Board board, Move move) {
        // Evaluating Ticket Used
        double movePenalty = 0;
        int destination = getDestination(move);

        final int SECRET_MOVE_PENALTY = -25;
        final int DOUBLE_MOVE_PENALTY = -50;
        final int REPEATED_MOVE_PENALTY = -30;

        if (alreadyVisitedLocations.contains(destination)) {
            movePenalty += REPEATED_MOVE_PENALTY;
        }

        if (((board.getMrXTravelLog().size() < board.getSetup().moves.size() / 2) || (!(board.getSetup().moves.get(board.getMrXTravelLog().size()))) && getMinDistance(move.source()) > 2)) {
            movePenalty += move.tickets().iterator().next() == ScotlandYard.Ticket.SECRET ? SECRET_MOVE_PENALTY : 0;
            movePenalty += (move instanceof Move.DoubleMove) ? DOUBLE_MOVE_PENALTY : 0;
        }

        return movePenalty;
    }


    private int calculateFreedomScore(Board board, int mrXLocation) {
        int totalSum = 0;

        try {
            Set<Integer> adjacentNodes = board.getSetup().graph.adjacentNodes(mrXLocation);
            for (Integer neighbour : adjacentNodes) {
                if (!occupiedLocations.contains(neighbour)) {
                    totalSum++; // if neighbor unoccupied count it as a freedom point
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return totalSum;
    }

//    An anonymous subclass is a one-time, unnamed extension of a class, used to
//    override behavior in a localized context. In my AI, I use an anonymous
//    subclass of BoardProxy to simulate detective moves by overriding
//    just getDetectiveLocation and getAvailableMoves, while inheriting all
//    other logic from the real board. This lets me safely simulate without
//    affecting the actual game state.


//    What this method below achieves:
//    You return a fake-but-functional board, where:
//
//    One detective has moved to a new location
//
//    Their move is simulated (without changing the real board)
//
//    The game is ready for the next detective to move, or for MrX if they're all done



    private Board simulateDetectiveMove(Piece detective, List<Piece> remaining, int mrXLocation, Board board, Move move) {
//        You’re simulating what happens if one detective makes a move.
        return new BoardProxy(board) {
            @Override
            @Nonnull
            public Optional<Integer> getDetectiveLocation(Piece.Detective detective2) {
                //If the detective we’re querying is the one being simulated:
                    //Return the destination of their move (where we expect them to go)
                if (detective.equals(detective2)) {
                    return Optional.of(getDestination(move));
                }
                //else just return the actual location from the original board
                return super.getDetectiveLocation(detective2);
            }

            @Override
            @Nonnull
            public ImmutableSet<Move> getAvailableMoves() {
                if (remaining.isEmpty()) {
                    return ImmutableSet.copyOf(getMrXMoves(board, mrXLocation, board.getSetup()));
                }
                return ImmutableSet.copyOf(getDetectiveMoves(board, remaining.get(0), board.getSetup()));
            }
        };
    }


//    So what does this method actually achieve?
//    It creates a fake board where:
//
//    It’s now a detective’s turn (after MrX just moved)
//
//    When asked for available moves, it gives only that detective’s legal moves
//
//    Everything else stays the same

    // -----------------------------------

    // why inner class

//    You want a board that behaves almost exactly like the real one…
//        But changes just one or two methods
//        So you use an anonymous subclass to:
//          Reuse all of BoardProxy's behavior
//          But override specific parts just for the simulation
//        And because this is done immutably — your original board remains untouched.

    private Board simulateMrXMove(List<Piece> remaining, Board board) {
//        Create a custom version of BoardProxy just for this one use...
//        ...and override a couple methods to simulate things differently
        return new BoardProxy(board) {

            @Override
            @Nonnull
            public ImmutableSet<Move> getAvailableMoves() {
                return ImmutableSet.copyOf(getDetectiveMoves(board, remaining.get(0), board.getSetup()));
                // get all legal moves of the detective who is going to move next
                //  This lets minimax simulate one detective's move at a time
            }
        };
        //simulateMrXMove returns a proxy board that simulates the start of the
        // detectives' phase after MrX has made a move. It overrides getAvailableMoves
        // to provide legal options only for the next detective in line, enabling clean,
        // immutable recursion through minimax without mutating the original board.
    }

    private Set<Move> getMrXMoves(Board board, int source, GameSetup setup) {
        if (!board.getWinner().isEmpty()) {
            return ImmutableSet.of();
        }
        Set<Move> moveSet = new HashSet<>(constructMrXMoves(source, setup, board));
        //generates all valid moves MrX can make from the current location
        return ImmutableSet.copyOf(moveSet);
    }

    private Set<Move> getDetectiveMoves(Board board, Piece detective, GameSetup setup) {
        if (!board.getWinner().isEmpty()) {
            return ImmutableSet.of();
        }
        Set<Move> moveSet = new HashSet<>(constructDetectiveMoves(board.getDetectiveLocation((Piece.Detective) detective).get(), detective, setup, board));
        return ImmutableSet.copyOf(moveSet);
    }

    private Set<Move> constructDetectiveMoves(int currentLocation, Piece detective, GameSetup setup, Board board) {
        Set<Move> playerMoves = new HashSet<>();

        for (int destination : setup.graph.adjacentNodes(currentLocation)) {
            if (!occupiedLocations.contains(destination)) {
                for (ScotlandYard.Transport transport : setup.graph.edgeValueOrDefault(currentLocation, destination, ImmutableSet.of())) {
                    if (board.getPlayerTickets(detective).get().getCount(transport.requiredTicket()) > 0) {
                        Move.SingleMove move = new Move.SingleMove(detective, currentLocation, transport.requiredTicket(), destination);
                        playerMoves.add(move);
                    }
                }
            }
        }
        return playerMoves;
    }

    private Set<Move> constructMrXMoves(int currentLocation, GameSetup setup, Board board) {
        Piece mrXPiece = null;
        for (Piece piece : players) {
            if (piece.isMrX()) {
                mrXPiece = piece;
            }
        }

//        I only generate single moves in my constructMrXMoves() method because I use
//        it solely for internal evaluation within my AI — not for determining the actual
//        legal moves MrX can make.
//        rest legal moves come from board.getAvailableMoves

        Set<Move> playerMoves = new HashSet<>();
        for (int destination : setup.graph.adjacentNodes(currentLocation)) {
            if (!occupiedLocations.contains(destination)) {
                for (ScotlandYard.Transport transport : setup.graph.edgeValueOrDefault(currentLocation, destination, ImmutableSet.of())) {
                    if (board.getPlayerTickets(mrXPiece).get().getCount(transport.requiredTicket()) > 0) {
                        Move.SingleMove move = new Move.SingleMove(mrXPiece, currentLocation, transport.requiredTicket(), destination);
                        playerMoves.add(move);
                    }
                }

            }
        }
        return playerMoves;
    }

    private double getMinDistance(int mrXLocation) {
        int minDistance = Integer.MAX_VALUE;
        for (int detectiveLocation : occupiedLocations) {
            int distance = shortestPaths[mrXLocation][detectiveLocation];
            minDistance = Math.min(distance, minDistance);
        }
        return minDistance;
    }

    private void precomputeShortestPaths(GameSetup setup) {
        // This method precomputes shortest path distances between every pair of nodes on the map
//        It’s precomputed because the graph doesn’t change
//        called once and Once is enough, and it gives me O(1) distance lookup during evaluation.
        int size = 200;
        shortestPaths = new int[size][size];
        //first is row second is column
        //shortestPaths[i][j] -> The shortest number of moves it takes to go from
        // node i to node j on the map.
        for (int i = 0; i < size; i++) {
            shortestPaths[i][i] = 0;

        }
        //diagonal matrix path to itself is zero

        for (int i = 1; i < size; i++) {
            for (int neighbor : setup.graph.adjacentNodes(i)) {
                shortestPaths[i][neighbor] = 1;
            }
        }

        // Floyd-Warshall Algorithm
        for (int k = 1; k < size; k++) {
            for (int i = 1; i < size; i++) {
                for (int j = 1; j < size; j++) {
                    shortestPaths[i][j] = Math.min(shortestPaths[i][j], shortestPaths[i][k] + shortestPaths[k][j]);
                }
            }
        }
    }

    private static class BoardProxy implements Board {
//        BoardProxy is your custom wrapper around the real game board.
//        it lets you
//          Simulate fake board states
//          Without changing the real board
//          And without needing to copy or clone everything manually
//        So i  t’s a Proxy and also acts like a lightweight Decorator.
        private final Board originalBoard;

        public BoardProxy(Board board) {
            this.originalBoard = board;
        }
//        You pass in the real board and store it.
//        This proxy now has access to everything the original board knows.

        @Nonnull
        @Override
        public GameSetup getSetup() {
            return originalBoard.getSetup();
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {
            return players;
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            return originalBoard.getDetectiveLocation(detective);
        }

        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            return originalBoard.getPlayerTickets(piece);
        }

        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return originalBoard.getMrXTravelLog();
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            return originalBoard.getWinner();
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            return originalBoard.getAvailableMoves();
        }
    }
}



// Proxy pattern
// Decorator pattern in subclasses used as we are enhancing implementation
// strategy pattern AI itself
// visitor pattern



//evaluateBoard() scores the current state — how safe or dangerous MrX’s position is
//evaluateMove() scores the nature of the move itself




/*
List -  An ordered collection that can contain duplicate elements.
        ordered means input order is saved and same as output order
        Elements can be accessed by their index

    ArrayList - Fast for random access. Slower for insertions/deletions in the middle.
                automatically increases size depending on input - dynamic

Set -   A collection that does not allow duplicates.
        No guaranteed order - input order != output order

    HashSet - No guaranteed order. Uses a hash table. Fast access.
              A hash table is a data structure that stores key-value pairs, and allows fast access
              to values based on their keys using a hashing mechanism.

    HashMap - A Map implementation that stores key-value pairs.
              Allows one null key and multiple null values.
              No guaranteed order of elements.
              Each key is unique and maps to exactly one value


              Hashing:
                    The key (like "apple") is passed through a hash function
                    (e.g. key.hashCode() in Java).

                    This generates a hash code (an integer).

                    That hash code is converted to an index in an internal array.
              Storing:

                    The value is stored at that index.

                    If two keys have the same index (called a collision), Java uses
                    something like a linked list or tree to store multiple values at that index.


              put(key, value)  -	Adds/updates a key-value pair
              get(key)	       -    Returns the value for the key
              remove(key)	   -    Removes the key-value pair

              What if two keys have the same hashCode?
                    --Java uses equals() to compare keys and resolve the conflict via chaining.

Queue - A FIFO (First-In-First-Out) collection
        Used for storing elements before processing them.
    PriorityQueue - Elements are ordered by natural ordering or a custom comparator.


The <> is called the diamond operator, and it's used with generics to define the type
of objects a class will hold.

 */


