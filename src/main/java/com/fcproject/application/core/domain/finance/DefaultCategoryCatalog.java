package com.fcproject.application.core.domain.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.DefaultCategoryKey;

import java.util.List;

public final class DefaultCategoryCatalog {
    private static final List<DefaultCategory> DEFAULTS = List.of(
            new DefaultCategory(DefaultCategoryKey.INCOME_SALARY, "Salario", CategoryKind.INCOME),
            new DefaultCategory(DefaultCategoryKey.INCOME_FREELANCE, "Freelance", CategoryKind.INCOME),
            new DefaultCategory(DefaultCategoryKey.INCOME_INVESTMENTS, "Investimentos", CategoryKind.INCOME),
            new DefaultCategory(DefaultCategoryKey.INCOME_REFUNDS, "Reembolsos", CategoryKind.INCOME),
            new DefaultCategory(DefaultCategoryKey.INCOME_OTHER, "Outras receitas", CategoryKind.INCOME),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_FOOD, "Alimentacao", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_GROCERIES, "Mercado", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_HOUSING, "Moradia", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_TRANSPORTATION, "Transporte", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_HEALTH, "Saude", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_EDUCATION, "Educacao", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_LEISURE, "Lazer", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_BILLS, "Contas", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_SHOPPING, "Compras", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_SUBSCRIPTIONS, "Assinaturas", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_TAXES, "Impostos", CategoryKind.EXPENSE),
            new DefaultCategory(DefaultCategoryKey.EXPENSE_OTHER, "Outras despesas", CategoryKind.EXPENSE)
    );

    private DefaultCategoryCatalog() {
    }

    public static List<DefaultCategory> all() {
        return DEFAULTS;
    }

    public record DefaultCategory(DefaultCategoryKey key, String name, CategoryKind kind) {
    }
}
