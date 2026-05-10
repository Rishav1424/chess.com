package app.chess.com.game;

import app.chess.com.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "game")
@Data
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn
    @Column(updatable = false, nullable = false)
    private User whitePlayer;

    @JoinColumn
    @Column(updatable = false, nullable = false)
    private User blackPlayer;

    @Column(updatable = false)
    @CreationTimestamp
    private Timestamp timestamp;

    @Column
    @UpdateTimestamp
    private Timestamp updated;

    @Column
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ONGOING'")
    private GameStatus status;

    @Column
    private String[] moves;

}

