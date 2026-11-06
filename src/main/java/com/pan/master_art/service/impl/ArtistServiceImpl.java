package com.pan.master_art.service.impl;

import com.pan.master_art.entity.Artist;
import com.pan.master_art.repository.ArtistRepository;
import com.pan.master_art.service.ArtistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 艺术家服务实现类
 */
@Service
public class ArtistServiceImpl implements ArtistService {

    @Autowired
    private ArtistRepository artistRepository;


    @Override
    public Artist findById(Long id) {
        return artistRepository.findById(id).orElse(null);
    }
    @Override
    public void increaseViewCount(Long id) {
        Artist artist = artistRepository.findById(id).orElseThrow(() -> new RuntimeException("艺术家未找到"));
        artist.setWeight(artist.getWeight() + 1);
        artistRepository.save(artist);
    }

    @Override
    public Page<Artist> findByPeriod(String period, Pageable pageable) {
        return artistRepository.findByCreationPeriod(period, pageable);
    }

    @Override
    public Page<Artist> findMostViewed(Pageable pageable) {
        // 按照权重（weight）降序排序查询艺术家
        // 确保pageable中已经包含了按weight降序的排序
        // 如果没有，我们仍然可以使用传入的分页参数，因为在Controller中已经设置了排序
        return artistRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Artist save(Artist artist) {
        return artistRepository.save(artist);
    }
    
    @Override
    @Transactional
    public Artist update(Artist artist) {
        Artist existingArtist = findById(artist.getId());
        if (existingArtist == null) {
            return null;
        }
        
        // 保留创建时间
        artist.setCreateTime(existingArtist.getCreateTime());
        
        return artistRepository.save(artist);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        artistRepository.deleteById(id);
    }
    
    @Override
    public Page<Artist> findAll(Pageable pageable) {
        return artistRepository.findAll(pageable);
    }
    
    @Override
    public Page<Artist> findByName(String name, Pageable pageable) {
        return artistRepository.findByNameContainingOrEnglishNameContaining(name, name, pageable);
    }
    
    @Override
    public Page<Artist> findByNationality(String nationality, Pageable pageable) {
        return artistRepository.findByNationality(nationality, pageable);
    }
    
    @Override
    public Page<Artist> findByGenre(String genre, Pageable pageable) {
        return artistRepository.findByGenre(genre, pageable);
    }
    
    @Override
    public Page<Artist> findByCreationPeriod(String creationPeriod, Pageable pageable) {
        return artistRepository.findByCreationPeriod(creationPeriod, pageable);
    }
    
    @Override
    public Page<Artist> findByStatus(Integer status, Pageable pageable) {
        return artistRepository.findByStatus(status, pageable);
    }
    
    @Override
    public List<String> findAllGenres() {
        return artistRepository.findAllGenres();
    }
    
    @Override
    public List<String> findAllNationalities() {
        return artistRepository.findAllNationalities();
    }
    
    @Override
    public List<String> findAllCreationPeriods() {
        return artistRepository.findAllCreationPeriods();
    }
    
    @Override
    public Page<Artist> findByConditions(String name, String nationality, String genre, String creationPeriod, Pageable pageable) {
        return artistRepository.findByConditions(name, nationality, genre, creationPeriod, pageable);
    }
    
    @Override
    @Transactional
    public Artist updateStatus(Long id, Integer status) {
        Artist artist = findById(id);
        if (artist == null) {
            return null;
        }
        artist.setStatus(status);
        return artistRepository.save(artist);
    }
    
    @Override
    @Transactional
    public void increaseWeight(Long id, Integer increment) {
        Artist artist = findById(id);
        if (artist != null) {
            artist.setWeight(artist.getWeight() + increment);
            artistRepository.save(artist);
        }
    }
}