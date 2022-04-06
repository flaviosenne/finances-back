package com.project.finances.domain.usecases.category;

import com.project.finances.domain.entity.Category;
import com.project.finances.domain.entity.User;
import com.project.finances.domain.exception.BadRequestException;
import com.project.finances.domain.protocols.CategoryManagerProtocol;
import com.project.finances.domain.usecases.category.dto.CategoryDto;
import com.project.finances.domain.usecases.category.repository.CategoryCommand;
import com.project.finances.domain.usecases.category.repository.CategoryQuery;
import com.project.finances.domain.usecases.user.repository.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerCategory implements CategoryManagerProtocol {
    private final CategoryQuery categoryQuery;
    private final CategoryCommand categoryCommand;
    private final UserQuery userQuery;

    @Override
    public CategoryDto create(CategoryDto dto, String userId) {
        User user = userQuery.findByIdIsActive(userId).orElseThrow(()-> new BadRequestException("Usuário não informado para categoria"));

        Category categoryToSave = CategoryDto.of(dto).withUser(user);

        Category categorySaved = categoryCommand.save(categoryToSave);

        return CategoryDto.of(categorySaved);
    }

    @Override
    public List<CategoryDto> getCategories(String userId, String description) {
        return categoryQuery.getCategoriesByUser(userId, description).stream().map(CategoryDto::of).collect(Collectors.toList());
    }

    @Override
    public CategoryDto update(CategoryDto dto, String userId) {
        Category categoryToUpdate = categoryQuery.getCategoryByIdAndByUserId(dto.getId(), userId)
                .orElseThrow(()-> new BadRequestException("Categoria não encontrada"));

        categoryToUpdate.withDescription(dto.getDescription());

        return CategoryDto.of(categoryCommand.save(categoryToUpdate));
    }
}
