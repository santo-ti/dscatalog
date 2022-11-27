package com.kti.dscatalog.services;

import com.kti.dscatalog.dto.ProductDTO;
import com.kti.dscatalog.entities.Category;
import com.kti.dscatalog.entities.Product;
import com.kti.dscatalog.repositories.CategoryRepository;
import com.kti.dscatalog.repositories.ProductRepository;
import com.kti.dscatalog.services.exceptions.DatabaseException;
import com.kti.dscatalog.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductDTO> findAllPaged(PageRequest pageRequest) {
        Page<Product> list = repository.findAll(pageRequest);
        return list.map(ProductDTO::new);
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        Product entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        return new ProductDTO(entity, entity.getCategories());
    }

    @Transactional
    public ProductDTO insert(ProductDTO dto) {
        Product entity = new Product();
        copyDtoToEntity(dto, entity);
        entity = repository.save(entity);
        return new ProductDTO(entity, entity.getCategories());
    }

    @Transactional
    public ProductDTO update(Long id, ProductDTO dto) {
        verifyExistsById(id);
        Product entity = repository.getReferenceById(id);
        copyDtoToEntity(dto, entity);
        entity = repository.save(entity);
        return new ProductDTO(entity, entity.getCategories());
    }

    public void delete(Long id) {
        verifyExistsById(id);
        try {
            repository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            throw new DatabaseException("Integrity violation");
        }
    }

    private void copyDtoToEntity(ProductDTO dto, Product entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setPrice(dto.getPrice());
        entity.setImgUrl(dto.getImgUrl());
        entity.setDate(dto.getDate());

        entity.getCategories().clear();
        dto.getCategories().forEach(categoryDTO -> {
            Long categoryId = categoryDTO.getId();
            if (!categoryRepository.existsById(categoryId)) {
                throw new ResourceNotFoundException("Category id not found " + categoryId);
            }
            Category category = categoryRepository.getReferenceById(categoryId);
            entity.getCategories().add(category);
        });
    }

    private void verifyExistsById(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Id not found " + id);
        }
    }
}
