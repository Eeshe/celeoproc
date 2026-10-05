package me.eeshe.celeoproc.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import me.eeshe.celeoproc.model.ElectricityStatusEmbed;

/**
 * Persists {@link ElectricityStatusEmbed} entities.
 */
public interface ElectricityStatusEmbedRepository extends Repository {

    /**
     * Inserts the given embed, or updates the existing row when the message id is
     * already present.
     *
     * @param statusEmbed embed to persist
     */
    void save(ElectricityStatusEmbed statusEmbed);

    /**
     * @param messageId id of the Discord message the embed belongs to
     * @return the stored embed, or an empty optional when no row exists
     */
    Optional<ElectricityStatusEmbed> get(long messageId);

    /**
     * @param updatedAtOrBefore instant to compare against each embed's last update
     * @return every stored embed whose {@code updated_at} is at or before the given
     *         instant
     */
    List<ElectricityStatusEmbed> getStaleEmbeds(Instant updatedAtOrBefore);

    /**
     * Deletes the stored embed for the given message id, if present.
     *
     * @param messageId id of the Discord message the embed belongs to
     */
    void delete(long messageId);
}
