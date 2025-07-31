package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
    @Nonnull
    @Override
    public Model build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
        return new MyModel(setup, mrX, detectives);
    }
}

class MyModel implements Model {
    private final Set<Observer> observers = new HashSet<>();
    private final MyBoard board;


    public MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
        this.board = new MyBoard(setup, mrX, detectives);
    }

    @Nonnull
    @Override
    public Board getCurrentBoard() {
        return board;
    }

    @Override
    public void registerObserver(@Nonnull Observer observer) {
        if (observer == null) {
            throw new NullPointerException("Observer cannot be null");
        }
        if (!observers.add(observer)) {
            throw new IllegalArgumentException("observer already registered");
        }
    }

    @Override
    public void unregisterObserver(@Nonnull Observer observer) {
        if (observer == null) {
            throw new NullPointerException("observer is null");
        }
        if (!observers.remove(observer)) {
            throw new IllegalArgumentException("observer not registered");
        }
    }

    @Nonnull
    @Override
    public ImmutableSet<Observer> getObservers() {
        return ImmutableSet.copyOf(observers);
    }

    @Override
    public void chooseMove(@Nonnull Move move) {
        board.advance(move);

        Observer.Event event;
        if (board.getWinner().isEmpty()) {
            event = Observer.Event.MOVE_MADE;
        } else {
            event = Observer.Event.GAME_OVER;
        }

        for (Observer observer : observers) {
            observer.onModelChanged(board, event);
        }
    }

    public static final class MyBoard implements Board {

        private GameState state;

        public MyBoard(@Nonnull GameSetup setup, @Nonnull Player mrX, @Nonnull ImmutableList<Player> detectives) {
            this.state = new MyGameStateFactory().build(setup, mrX, detectives);
        }

//        This line creates an instance of MyGameStateFactory, then calls its
//        build(...) method with the game setup, MrX, and detectives. The result is
//        a fully initialized GameState object representing the starting state of
//        the game, which is stored in the state variable of MyBoard. This state is
//        then used to answer all board-related queries and to progress the game forward.”


        @Nonnull
        @Override
        public GameSetup getSetup() {
            return state.getSetup();
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {
            return state.getPlayers();
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            return state.getDetectiveLocation(detective);
        }

        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            return state.getPlayerTickets(piece);
        }

        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return state.getMrXTravelLog();
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            return state.getWinner();
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            return state.getAvailableMoves();
        }

        public void advance(Move move) {
            this.state = state.advance(move);
        }
    }
}
