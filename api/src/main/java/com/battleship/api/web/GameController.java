package com.battleship.api.web;

import com.battleship.api.application.GameService;
import com.battleship.api.application.GameView;
import com.battleship.api.application.JoinResult;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("/api")
public class GameController {

    public static final String TOKEN_HEADER = "X-Player-Token";

    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    /** Find an opponent: joins the oldest waiting game or opens a new one. */
    @PostMapping("/matchmaking")
    public ResponseEntity<JoinResponse> join(@Valid @RequestBody JoinRequest request) {
        JoinResult result = service.join(request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new JoinResponse(result.gameId(), result.token(), result.slot()));
    }

    /**
     * Current state for the calling player. Clients poll this; the ETag is the game version, so an unchanged
     * game answers 304 with no body. Vary on the token keeps two players that share a browser cache apart.
     */
    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameView> get(@PathVariable UUID gameId,
                                        @RequestHeader(TOKEN_HEADER) UUID token,
                                        WebRequest request) {
        GameView view = service.view(gameId, token);
        if (request.checkNotModified("\"" + view.version() + "\"")) {
            return null; // 304 already written
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.VARY, TOKEN_HEADER)
                .body(view);
    }

    @PutMapping("/games/{gameId}/fleet")
    public GameView placeFleet(@PathVariable UUID gameId,
                               @RequestHeader(TOKEN_HEADER) UUID token,
                               @Valid @RequestBody PlaceFleetRequest request) {
        return service.placeFleet(gameId, token, request.ships());
    }

    @PostMapping("/games/{gameId}/shots")
    public GameView fire(@PathVariable UUID gameId,
                         @RequestHeader(TOKEN_HEADER) UUID token,
                         @Valid @RequestBody FireRequest request) {
        return service.fire(gameId, token, request.row(), request.col());
    }
}
