//Copyright -- Aayush and Amaan
//should not be shared without our consent

package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */

public final class MyGameStateFactory implements Factory<GameState> {

    // implementing a generic factory that builds objects of type GameState

    private ImmutableSet<Move> getMoves(Player mrX, List<Player> detectives, GameSetup setup) {
        Set<Move> moveHashSet = new HashSet<>(calculatePlayerSingleMoves(mrX, setup, detectives));
        for (Player detective : detectives) {
            moveHashSet.addAll(calculatePlayerSingleMoves(detective, setup, detectives));
        }
        moveHashSet.addAll(calculateMrXDoubleMoves(mrX, setup, detectives));
        return ImmutableSet.copyOf(moveHashSet);
    }

    private Set<Integer> getOccupiedLocations(List<Player> detectives) {
        Set<Integer> occupiedLocations = new HashSet<>();
        for (Player detective : detectives) {
            occupiedLocations.add(detective.location());
        }
        return occupiedLocations;
        // MrX cannot move to a location that is currently occupied by a detective.
        // Likewise, detectives shouldn’t be able to move into each other’s space.
    }


    private Set<Move> calculatePlayerSingleMoves(Player player, GameSetup setup, List<Player> detectives) {
        Set<Move> playerMoves = new HashSet<>();
        int currentLocation = player.location();
        Set<Integer> occupiedLocations = getOccupiedLocations(detectives);

        for (int destination : setup.graph.adjacentNodes(currentLocation)) {
            if (!occupiedLocations.contains(destination)) {
                for (Transport transport : setup.graph.edgeValueOrDefault(currentLocation, destination, ImmutableSet.of())) {
                    // edgeValueOrDefault returns an ImmutableSet of Transport types — or an
                    // empty set if there's no direct edge.
                    if (player.has(transport.requiredTicket())) {
                        SingleMove move = new Move.SingleMove(player.piece(), currentLocation, transport.requiredTicket(), destination);
                        playerMoves.add(move);
                    }
                }
                if (player.isMrX() && player.has(Ticket.SECRET)) {
                    SingleMove move = new Move.SingleMove(player.piece(), currentLocation, Ticket.SECRET, destination);
                    playerMoves.add(move);
                }
            }
        }
        return playerMoves;
    }

    private Set<Move> calculateMrXDoubleMoves(Player mrX, GameSetup setup, List<Player> detectives) {
        Set<Move> mrXMoves = new HashSet<>();
        if (!mrX.has(Ticket.DOUBLE)) return mrXMoves;
        int currentLocation = mrX.location();
        Set<Integer> occupiedLocations = getOccupiedLocations(detectives);

        for (int destination1 : setup.graph.adjacentNodes(currentLocation)) {
            if (!occupiedLocations.contains(destination1)) {
                for (Transport transport1 : setup.graph.edgeValueOrDefault(currentLocation, destination1, ImmutableSet.of())) {
                    for (int destination2 : setup.graph.adjacentNodes(destination1)) {
                        if (!occupiedLocations.contains(destination2)) {
                            for (Transport transport2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
                                if (mrX.has(transport1.requiredTicket()) && mrX.has(Ticket.SECRET)) {
                                    DoubleMove move = new DoubleMove(mrX.piece(), currentLocation, transport1.requiredTicket(), destination1, Ticket.SECRET, destination2);
                                    mrXMoves.add(move);
                                }
                                if (mrX.has(transport2.requiredTicket()) && mrX.has(Ticket.SECRET)) {
                                    DoubleMove move = new DoubleMove(mrX.piece(), currentLocation, Ticket.SECRET, destination1, transport2.requiredTicket(), destination2);
                                    mrXMoves.add(move);
                                }
                                if (mrX.has(transport1.requiredTicket()) && mrX.has(transport2.requiredTicket())) {
                                    if (transport1.requiredTicket().equals(transport2.requiredTicket())) {
                                        if (mrX.tickets().getOrDefault(transport1.requiredTicket(), 0) >= 2) {
                                            DoubleMove move = new DoubleMove(mrX.piece(), currentLocation, transport1.requiredTicket(), destination1, transport2.requiredTicket(), destination2);
                                            mrXMoves.add(move);
                                        }
                                    } else {
                                        DoubleMove move = new DoubleMove(mrX.piece(), currentLocation, transport1.requiredTicket(), destination1, transport2.requiredTicket(), destination2);
                                        mrXMoves.add(move);
                                    }
                                }
                            }
                            if (mrX.tickets().getOrDefault(Ticket.SECRET, 0) >= 2) {
                                DoubleMove move = new DoubleMove(mrX.piece(), currentLocation, Ticket.SECRET, destination1, Ticket.SECRET, destination2);
                                mrXMoves.add(move);
                            }
                        }
                    }
                }
            }
        }
        return mrXMoves;
    }

    @Nonnull
    @Override
    public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
        if (mrX == null) {
            throw new NullPointerException("MrX is null!");
        }
        if (detectives == null) {
            throw new NullPointerException("Detectives list is null!");
        }

        Set<Integer> detectiveLocations = new HashSet<>();
        // HashSet because fast lookups O(1) on average
        // no duplicates
        for (Player detective : detectives) {
            if (!detectiveLocations.add(detective.location())) {
                throw new IllegalArgumentException("Location overlap between detectives detected!");
            }
            if (detective.has(Ticket.SECRET) || detective.has(Ticket.DOUBLE)) {
                throw new IllegalArgumentException("Detective has an invalid ticket!");
            }
        }
        Set<Piece> pieces = new HashSet<>();
        pieces.add(Piece.MrX.MRX);
        for (Player detective : detectives) {
            pieces.add(detective.piece());
        }

        ImmutableSet<Move> actualMoves = getMoves(mrX, detectives, setup);
        return new MyGameState(setup, ImmutableSet.copyOf(pieces), ImmutableList.of(), mrX, detectives, actualMoves);
    }

    private final class MyGameState implements GameState {
        private final GameSetup setup;
        private final ImmutableSet<Piece> remaining;
        private final ImmutableList<LogEntry> log;
        private final Player mrX;
        private final List<Player> detectives;
        private final ImmutableSet<Move> moves;
        private final ImmutableSet<Piece> winner;
        private final Map<Piece.Detective, Integer> detectiveLocations;
        private final ImmutableSet<Move> moveHistory;

        private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final List<Player> detectives, ImmutableSet<Move> moveHistory) {
            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;
            this.moveHistory = moveHistory;
            this.winner = checkGameOver();
            this.moves = getMoves(mrX, detectives, setup);

            if (setup.moves.isEmpty()) {
                throw new IllegalArgumentException("Moves is empty!");
            }

            this.detectiveLocations = new HashMap<>();
            for (Player detective : detectives) {
                detectiveLocations.put((Piece.Detective) detective.piece(), detective.location());
            }

            if (mrX.tickets().isEmpty()) {
                throw new IllegalArgumentException("MrX tickets are empty!");
            }

            if (detectives.isEmpty()) {
                throw new IllegalArgumentException("Detectives list is empty!");
            }

            if (remaining.stream().filter(piece -> piece instanceof Piece.MrX).count() > 1) {
                throw new IllegalArgumentException("There may only be one MrX piece");
            }

            Set<Player> detectiveSet = new HashSet<>(detectives);
            // hashset removes duplicates
            if (detectiveSet.size() != detectives.size()) {
                throw new IllegalArgumentException("Duplicate detectives found!");
            }

        }

        private ImmutableSet<Piece> checkGameOver() {
            if (mrXIsCaught() || !canPlayerMove(mrX)) {
                Set<Piece> detectivePieces = new HashSet<>();
                for (Player detective : detectives) {
                    detectivePieces.add(detective.piece());
                }
                return ImmutableSet.copyOf(detectivePieces);
            }
            if (mrXEscaped() || allDetectiveMovesUsed()) {
                return ImmutableSet.of(mrX.piece());
            }
            return ImmutableSet.of();
        }

        private boolean allDetectiveMovesUsed() {
            for (Player detective : detectives) {
                if (canPlayerMove(detective)) {
                    return false;
                }
            }
            return true;
        }

        private boolean mrXIsCaught() {
            for (Player detective : detectives) {
                if (detective.location() == mrX.location()) {
                    return true;
                }
            }
            return false;
        }

        private boolean mrXEscaped() {
            return log.size() >= setup.moves.size();
        }

        private boolean canPlayerMove(Player player) {
            Set<Integer> occupiedLocations = new HashSet<>();
            for (Player detective : detectives) {
                occupiedLocations.add(detective.location());
            }
            int currentLocation = player.location();
            for (int destination : setup.graph.adjacentNodes(currentLocation)) {
                for (Transport t : setup.graph.edgeValueOrDefault(currentLocation, destination, ImmutableSet.of())) {
                    if ((player.has(t.requiredTicket()) || (player.isMrX() && player.has(Ticket.SECRET))) && !occupiedLocations.contains(destination)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public ImmutableSet<Move> updateMoves(Move newMove) {
            List<Move> updatedMovesList = new ArrayList<>(moveHistory);
            updatedMovesList.add(newMove);
            return ImmutableSet.copyOf(updatedMovesList);
        }

        @Nonnull
        @Override
        public GameState advance(Move move) {
            if (!moves.contains(move)) {
                throw new IllegalArgumentException("Move is not allowed in the current game state.");
            }

//            Using instanceof would make the code more tightly coupled and harder to extend.
//            would have to use if else for when I used instanceof
//            The Visitor Pattern is a clean OOP way to handle multiple subclasses without
//            modifying them. It follows the Open/Closed Principle — open for extension,
//            closed for modification.

            return move.accept(new Move.Visitor<>() {
                public GameState visit(SingleMove move) {
                    if (move.commencedBy().isMrX()) {
                        return handleMrXSingleMove(move);
                    } else {
                        return handleDetectiveSingleMove(move);
                    }
                }

                public GameState visit(DoubleMove move) {
                    return handleMrXDoubleMove(move);
                }
            });
        }

//        An immutable object is one whose state cannot be changed after it is created.
//        Once the object is constructed, its fields remain constant and no setter or
//        modifier methods can alter its data.

//        Immutability provides thread safety and reduces bugs caused by accidental changes

//        if I do String.toUpperCase , a new object is returned. Original is not changed
//        Immutability follows the principle of defensive programming —
//        once the object is created, its integrity is guaranteed.
//        integrity means - what u built is what u get

        private GameState handleMrXSingleMove(SingleMove move) {
            ImmutableList<LogEntry> updatedLog = updateMrXTravelLog(move);
            Player updatedMrX = updateMrXLocation(move);

            Set<Piece> newRemaining = new HashSet<>();
            for (Player detective : detectives) {
                newRemaining.add(detective.piece());
            }
            ImmutableSet<Piece> remaining = ImmutableSet.copyOf(newRemaining);

            return new MyGameState(setup, remaining, updatedLog, updatedMrX, detectives, updateMoves(move));
        }

        private GameState handleDetectiveSingleMove(SingleMove move) {
            List<Player> updatedDetectives = updateDetectiveList(move);
            // updatedDetectives contains detectives after their ticket count has
            // decreased and location is updated

            Player updatedMrX = mrX.give(move.ticket);

            Set<Piece> newRemaining = new HashSet<>();
            for (Piece piece : remaining) {
                if (!piece.equals(move.commencedBy())) {
                    newRemaining.add(piece);
                }
            }
            if (newRemaining.isEmpty()) {
                newRemaining.add(Piece.MrX.MRX);
            }
            ImmutableSet<Piece> remaining = ImmutableSet.copyOf(newRemaining);

            return new MyGameState(setup, remaining, getMrXTravelLog(), updatedMrX, updatedDetectives, updateMoves(move));
        }

        private GameState handleMrXDoubleMove(DoubleMove move) {
            ImmutableList<LogEntry> updatedLog = updateMrXTravelLogForDouble(move);
            Player updatedMrX = updateMrXLocationForDouble(move);


            Set<Piece> newRemaining = new HashSet<>();
            for (Player detective : detectives) {
                newRemaining.add(detective.piece());
            }

            ImmutableSet<Piece> remaining = ImmutableSet.copyOf(newRemaining);

            return new MyGameState(setup, remaining, updatedLog, updatedMrX, detectives, updateMoves(move));
        }

        private ImmutableList<LogEntry> updateMrXTravelLog(Move.SingleMove move) {
            List<LogEntry> updatedLog = new ArrayList<>(log);
            boolean isRevealRound = updatedLog.size() < setup.moves.size() && setup.moves.get(updatedLog.size());
            //If we're still within 24 rounds,
            // and this specific round is marked as true (i.e. it's a reveal round)
            if (isRevealRound) {
                updatedLog.add(LogEntry.reveal(move.ticket, move.destination));
            } else updatedLog.add(LogEntry.hidden(move.ticket));
            return ImmutableList.copyOf(updatedLog);
        }

        private ImmutableList<LogEntry> updateMrXTravelLogForDouble(Move.DoubleMove move) {
            List<LogEntry> updatedLog = new ArrayList<>(log);
            boolean isRevealRound = updatedLog.size() < setup.moves.size() && setup.moves.get(updatedLog.size());

            if (isRevealRound) {
                updatedLog.add(LogEntry.reveal(move.ticket1, move.destination1));
            } else updatedLog.add(LogEntry.hidden(move.ticket1));

            isRevealRound = updatedLog.size() < setup.moves.size() && setup.moves.get(updatedLog.size());
            if (isRevealRound) {
                updatedLog.add(LogEntry.reveal(move.ticket2, move.destination2));
            } else updatedLog.add(LogEntry.hidden(move.ticket2));
            return ImmutableList.copyOf(updatedLog);
        }

        //deducts the ticket and then updates location and creates new player object for MrX
        private Player updateMrXLocation(Move.SingleMove move) {
            return mrX.use(move.ticket).at(move.destination);
        }

        private Player updateMrXLocationForDouble(Move.DoubleMove move) {
            return mrX.use(move.ticket1).at(move.destination1).use(move.ticket2).at(move.destination2).use(Ticket.DOUBLE);
        }

        private List<Player> updateDetectiveList(Move.SingleMove move) {
            List<Player> updatedDetectives = new ArrayList<>();
            for (Player detective : detectives) {
                if (detective.piece().equals(move.commencedBy())) {
                    updatedDetectives.add(detective.use(move.ticket).at(move.destination));
                } else {
                    updatedDetectives.add(detective);
                }
            }
            return updatedDetectives;
        }

        @Nonnull
        @Override
        public GameSetup getSetup() {
            return setup;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {
            return ImmutableSet.copyOf(this.remaining);
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            Integer location = detectiveLocations.get(detective);
            if (location == null) {
                return Optional.empty();
            } else {
                return Optional.of(location);
            }
        }

        @Nonnull
        @Override
        // TicketBoard says that if u give me Ticket type ill give u how many that
        // player has got left
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            if (piece.isMrX()) {
                if (mrX != null && mrX.piece().equals(piece)) {
                    return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
                }
            } else if (piece.isDetective()) {
                for (Player detective : detectives) {
                    if (detective.piece().equals(piece)) {
                        return Optional.of(ticket -> detective.tickets().getOrDefault(ticket, 0));
                    }
                }

            }
            return Optional.empty();
        }

        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return log;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            return this.winner;
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            return calculateAvailableMoves();
        }

        private ImmutableSet<Move> calculateAvailableMoves() {
            if (!this.winner.isEmpty()) {
                return ImmutableSet.of();
            }

            Set<Move> moveSet = new HashSet<>();
            if (remaining.contains(mrX.piece())) {
                moveSet.addAll(calculatePlayerSingleMoves(mrX, setup, detectives));
                if (!(log.size() + 2 > setup.moves.size())) moveSet.addAll(calculateMrXDoubleMoves(mrX, setup, detectives));
                // here UI gets a choice to play double or single move
            } else {
                for (Player detective : detectives) {
                    if (remaining.contains(detective.piece())) {
                        moveSet.addAll(calculatePlayerSingleMoves(detective, setup, detectives));
                    }
                }
                if (moveSet.isEmpty()) {
                    moveSet.addAll(calculatePlayerSingleMoves(mrX, setup, detectives));
                    if (!(log.size() + 2 > setup.moves.size()))
                        moveSet.addAll(calculateMrXDoubleMoves(mrX, setup, detectives));
                }
            }
            return ImmutableSet.copyOf(moveSet);
        }
    }
}
