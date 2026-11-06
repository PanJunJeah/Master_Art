package com.pan.master_art.service.impl;

import com.pan.master_art.entity.Artist;
import com.pan.master_art.entity.Artwork;
import com.pan.master_art.repository.ArtworkRepository;
import com.pan.master_art.service.ArtworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 艺术品服务实现类
 */
@Service
public class ArtworkServiceImpl implements ArtworkService {

    @Autowired
    private ArtworkRepository artworkRepository;
    
    @Override
    public Artwork findById(Long id) {
        return artworkRepository.findById(id).orElse(null);
    }
    
    @Override
    @Transactional
    public Artwork save(Artwork artwork) {
        return artworkRepository.save(artwork);
    }
    
    @Override
    @Transactional
    public Artwork update(Artwork artwork) {
        Artwork existingArtwork = findById(artwork.getId());
        if (existingArtwork == null) {
            return null;
        }
        
        // 保留创建时间
        artwork.setCreateTime(existingArtwork.getCreateTime());
        
        return artworkRepository.save(artwork);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        artworkRepository.deleteById(id);
    }
    
    @Override
    public Page<Artwork> findAll(Pageable pageable) {
        return artworkRepository.findAll(pageable);
    }
    
    @Override
    public Page<Artwork> findByName(String name, Pageable pageable) {
        return artworkRepository.findByNameContainingOrEnglishNameContaining(name, name, pageable);
    }
    
    @Override
    public Page<Artwork> findByArtistId(Long artistId, Pageable pageable) {
        Artist artist = new Artist();
        artist.setId(artistId);
        return artworkRepository.findByArtist(artist, pageable);
    }
    
    @Override
    public Page<Artwork> findByType(Integer type, Pageable pageable) {
        return artworkRepository.findByType(type, pageable);
    }
    
    @Override
    public Page<Artwork> findByGenre(String genre, Pageable pageable) {
        return artworkRepository.findByGenre(genre, pageable);
    }
    
    @Override
    public Page<Artwork> findByCreationYear(String creationYear, Pageable pageable) {
        return artworkRepository.findByCreationYear(creationYear, pageable);
    }
    
    @Override
    public Page<Artwork> findByStatus(Integer status, Pageable pageable) {
        return artworkRepository.findByStatus(status, pageable);
    }
    
    @Override
    public List<Integer> findAllTypes() {
        return artworkRepository.findAllTypes();
    }
    
    @Override
    public List<String> findAllGenres() {
        return artworkRepository.findAllGenres();
    }
    
    @Override
    public List<String> findAllCreationYears() {
        return artworkRepository.findAllCreationYears();
    }
    
    @Override
    public List<String> findAllMaterials() {
        return artworkRepository.findAllMaterials();
    }
    
    @Override
    public Page<Artwork> findByConditions(String name, Long artistId, Integer type, String genre, String creationYear, Pageable pageable) {
        return artworkRepository.findByConditions(name, artistId, type, genre, creationYear, pageable);
    }
    
    @Override
    public Page<Artwork> findMostViewed(Pageable pageable) {
        return artworkRepository.findByStatusOrderByViewCountDesc(1, pageable);
    }
    
    @Override
    public Page<Artwork> findByCollectionPlace(String collectionPlace, Pageable pageable) {
        return artworkRepository.findByCollectionPlace(collectionPlace, pageable);
    }
    
    @Override
    @Transactional
    public Artwork updateStatus(Long id, Integer status) {
        Artwork artwork = findById(id);
        if (artwork == null) {
            return null;
        }
        artwork.setStatus(status);
        return artworkRepository.save(artwork);
    }
    
    @Override
    @Transactional
    public void increaseViewCount(Long id) {
        Artwork artwork = findById(id);
        if (artwork != null) {
            artwork.setViewCount(artwork.getViewCount() + 1);
            artworkRepository.save(artwork);
        }
    }
    
    @Override
    @Transactional
    public void increaseWeight(Long id, Integer increment) {
        Artwork artwork = findById(id);
        if (artwork != null) {
            artwork.setWeight(artwork.getWeight() + increment);
            artworkRepository.save(artwork);
        }
    }
}