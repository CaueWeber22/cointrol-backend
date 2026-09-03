package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.finance.FinanceRequests.CreateCategoryRequest;
import com.fcproject.adapters.inbound.dto.finance.FinanceRequests.UpdateCategoryRequest;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.CategoryResponse;
import com.fcproject.adapters.inbound.security.CurrentUserIdProvider;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateCategory;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateCategory;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.ports.inbound.finance.ArchiveCategoryInPort;
import com.fcproject.application.ports.inbound.finance.CreateCategoryInPort;
import com.fcproject.application.ports.inbound.finance.ListCategoriesInPort;
import com.fcproject.application.ports.inbound.finance.UpdateCategoryInPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CreateCategoryInPort createCategory;
    private final ListCategoriesInPort listCategories;
    private final UpdateCategoryInPort updateCategory;
    private final ArchiveCategoryInPort archiveCategory;
    private final CurrentUserIdProvider currentUser;

    public CategoryController(
            CreateCategoryInPort createCategory,
            ListCategoriesInPort listCategories,
            UpdateCategoryInPort updateCategory,
            ArchiveCategoryInPort archiveCategory,
            CurrentUserIdProvider currentUser
    ) {
        this.createCategory = createCategory;
        this.listCategories = listCategories;
        this.updateCategory = updateCategory;
        this.archiveCategory = archiveCategory;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            Principal principal,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse response = CategoryResponse.from(createCategory.createCategory(new CreateCategory(
                currentUser.get(principal), request.name(), request.kind()
        )));
        return ResponseEntity.created(URI.create("/api/v1/categories/" + response.id())).body(response);
    }

    @GetMapping
    public List<CategoryResponse> list(
            Principal principal,
            @RequestParam(required = false) CategoryKind kind,
            @RequestParam(required = false) ResourceStatus status
    ) {
        return listCategories.listCategories(currentUser.get(principal), kind, status)
                .stream().map(CategoryResponse::from).toList();
    }

    @PatchMapping("/{id}")
    public CategoryResponse update(
            Principal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return CategoryResponse.from(updateCategory.updateCategory(
                new UpdateCategory(currentUser.get(principal), id, request.name())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(Principal principal, @PathVariable UUID id) {
        archiveCategory.archiveCategory(currentUser.get(principal), id);
        return ResponseEntity.noContent().build();
    }
}
