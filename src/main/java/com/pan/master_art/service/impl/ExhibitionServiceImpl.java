package com.pan.master_art.service.impl;

import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.repository.ExhibitionRepository;
import com.pan.master_art.service.ExhibitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 展览服务实现类
 */
@Service
public class ExhibitionServiceImpl implements ExhibitionService {

    @Autowired
    private ExhibitionRepository exhibitionRepository;
    
    @Override
    public Exhibition findById(Long id) {
        return exhibitionRepository.findById(id).orElse(null);
    }
    
    @Override
    @Transactional
    public Exhibition save(Exhibition exhibition) {
        // 设置创建时间
        if (exhibition.getCreateTime() == null) {
            exhibition.setCreateTime(new Date());
        }
        
        // 设置默认权重
        if (exhibition.getWeight() == null) {
            exhibition.setWeight(0);
        }
        
        // 设置默认浏览次数
        if (exhibition.getViewCount() == null) {
            exhibition.setViewCount(0);
        }
        
        return exhibitionRepository.save(exhibition);
    }
    
    @Override
    @Transactional
    public Exhibition update(Exhibition exhibition) {
        Exhibition existingExhibition = findById(exhibition.getId());
        if (existingExhibition == null) {
            return null;
        }
        
        // 保留创建时间
        exhibition.setCreateTime(existingExhibition.getCreateTime());
        
        // 保留浏览次数
        exhibition.setViewCount(existingExhibition.getViewCount());
        
        return exhibitionRepository.save(exhibition);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        exhibitionRepository.deleteById(id);
    }
    
    @Override
    public Page<Exhibition> findAll(Pageable pageable) {
        return exhibitionRepository.findAll(pageable);
    }
    
    @Override
    public Page<Exhibition> findByName(String name, Pageable pageable) {
        return exhibitionRepository.findByNameContainingOrEnglishNameContaining(name, name, pageable);
    }
    
    @Override
    public Page<Exhibition> findByType(Integer type, Pageable pageable) {
        return exhibitionRepository.findByType(type, pageable);
    }
    
    @Override
    public Page<Exhibition> findByTheme(String theme, Pageable pageable) {
        return exhibitionRepository.findByThemeContaining(theme, pageable);
    }
    
    @Override
    public Page<Exhibition> findByLocation(String location, Pageable pageable) {
        return exhibitionRepository.findByLocationContaining(location, pageable);
    }
    
    @Override
    public Page<Exhibition> findCurrentExhibitions(Pageable pageable) {
        return exhibitionRepository.findCurrentExhibitions(new Date(), Exhibition.ExhibitionStatus.ONGOING, pageable);
    }
    
    @Override
    public Page<Exhibition> findUpcomingExhibitions(Pageable pageable) {
        return exhibitionRepository.findUpcomingExhibitions(new Date(), Exhibition.ExhibitionStatus.UPCOMING, pageable);
    }
    
    @Override
    public Page<Exhibition> findPastExhibitions(Pageable pageable) {
        return exhibitionRepository.findPastExhibitions(new Date(), Exhibition.ExhibitionStatus.ENDED, pageable);
    }
    
    @Override
    public Page<Exhibition> findByCurator(String curator, Pageable pageable) {
        return exhibitionRepository.findByCuratorContaining(curator, pageable);
    }

    @Override
    public Page<Exhibition> findMostViewed(Integer status, Pageable pageable) {
        if (status == null) {
            // 如果状态为空，返回所有展览按浏览次数排序
            return exhibitionRepository.findAllByOrderByViewCountDesc(pageable);
        }
        // 将Integer状态转换为对应的Exhibition.ExhibitionStatus枚举值
        Exhibition.ExhibitionStatus statusEnum = convertIntStatusToString(status);
        return exhibitionRepository.findByStatusOrderByViewCountDesc(statusEnum, pageable);
    }

    @Override
    public Page<Exhibition> findMostViewed(Pageable pageable) {
        // 默认返回状态为ONGOING（进行中）的展览
        return findMostViewed(1, pageable);
    }
    
    @Override
    public Page<Exhibition> findByConditions(String name, Integer type, String location, Date startTime, Date endTime, Integer status, Pageable pageable) {
        return exhibitionRepository.findByConditions(
            name, 
            type, 
            location, 
            startTime, 
            endTime, 
            convertIntStatusToString(status), 
            pageable);
    }
    
    @Override
    public List<String> findAllLocations() {
        return exhibitionRepository.findAllLocations();
    }
    
    @Override
    public List<String> findAllThemes() {
        return exhibitionRepository.findAllThemes();
    }
    
    @Override
    @Transactional
    public Exhibition updateStatus(Long id, Integer status) {
        Exhibition exhibition = findById(id);
        if (exhibition == null) {
            return null;
        }
        // 将Integer状态转换为对应的String状态
        String statusStr = String.valueOf(convertIntStatusToString(status));
        exhibition.setStatus(Exhibition.ExhibitionStatus.valueOf(statusStr));
        return exhibitionRepository.save(exhibition);
    }
    
    @Override
    @Transactional
    public void increaseViewCount(Long id) {
        Exhibition exhibition = findById(id);
        if (exhibition != null) {
            exhibition.setViewCount(exhibition.getViewCount() + 1);
            exhibitionRepository.save(exhibition);
        }
    }
    
    @Override
    @Transactional
    public void increaseWeight(Long id, Integer increment) {
        Exhibition exhibition = findById(id);
        if (exhibition != null) {
            exhibition.setWeight(exhibition.getWeight() + increment);
            exhibitionRepository.save(exhibition);
        }
    }

    @Override
    @Transactional
    public void autoUpdateExhibitionStatus() {
        Date now = new Date();
        List<Exhibition> exhibitions = exhibitionRepository.findAll();
        
        for (Exhibition exhibition : exhibitions) {
            // 如果当前时间在展览开始时间之前，状态设为未开始(UPCOMING)
            if (now.before(exhibition.getStartTime())) {
                exhibition.setStatus(Exhibition.ExhibitionStatus.UPCOMING);
            }
            // 如果当前时间在展览开始和结束时间之间，状态设为进行中(ONGOING)
            else if (now.after(exhibition.getStartTime()) && now.before(exhibition.getEndTime())) {
                exhibition.setStatus(Exhibition.ExhibitionStatus.ONGOING);
            }
            // 如果当前时间在展览结束时间之后，状态设为已结束(ENDED)
            else if (now.after(exhibition.getEndTime())) {
                exhibition.setStatus(Exhibition.ExhibitionStatus.ENDED);
            }
        }
        
        // 批量保存更新后的展览状态
        exhibitionRepository.saveAll(exhibitions);
    }
    
    @Override
    public Page<Exhibition> findAllByOrderByViewCountDesc(Pageable pageable) {
        return exhibitionRepository.findAllByOrderByViewCountDesc(pageable);
    }
    
    /**
     * 将Integer状态值转换为对应的ExhibitionStatus枚举值
     */
    private Exhibition.ExhibitionStatus convertIntStatusToString(Integer status) {
        if (status == null) {
            return Exhibition.ExhibitionStatus.UPCOMING;
        }
        switch (status) {
            case 0:
                return Exhibition.ExhibitionStatus.UPCOMING;
            case 1:
                return Exhibition.ExhibitionStatus.ONGOING;
            case 2:
                return Exhibition.ExhibitionStatus.ENDED;
            default:
                return Exhibition.ExhibitionStatus.UPCOMING;
        }
    }
}