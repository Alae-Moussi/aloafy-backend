package com.bd.aloafy.repository;

import com.bd.aloafy.entity.PlaylistSong;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    @Modifying
    @Transactional
    void deleteBySongId(Long songId);

    boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);

    List<PlaylistSong> findByPlaylistIdOrderByPositionAsc(Long playlistId);

    Optional<PlaylistSong> findByPlaylistIdAndSongId(Long playlistId, Long songId);

    @Modifying
    @Transactional


    void deleteByPlaylistId(Long playlistId);
}

