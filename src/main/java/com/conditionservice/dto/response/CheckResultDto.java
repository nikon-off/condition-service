package com.conditionservice.dto.response;

/**
 * Результат проверки группы условий отбора.
 *
 * @param matched          {@code true}, если все условия группы являются подмножеством
 *                         переданного payload (с учётом правила «пустая группа → false»)
 * @param totalConditions  общее количество условий в группе
 * @param matchedCount     количество условий, которым соответствует переданный payload
 */
public record CheckResultDto(
        boolean matched,
        int totalConditions,
        int matchedCount) {
}