-- Phase 4 one-off data cleanup: remove polluted cross-city fallback rows
-- Goal: delete city_supermarket rows that were historically seeded from Bangkok fallback
-- into unrelated cities (e.g., London -> bigc/lotuss/tops).

-- 1) Preview suspect rows first
SELECT id, city, supermarket_name, official_website, catalog_search_url, notes
FROM city_supermarket
WHERE LOWER(city) <> 'bangkok'
  AND (
    LOWER(supermarket_name) IN ('big c', 'lotus''s', 'tops')
    OR LOWER(COALESCE(official_website, '')) LIKE '%bigc.co.th%'
    OR LOWER(COALESCE(official_website, '')) LIKE '%lotuss.com%'
    OR LOWER(COALESCE(official_website, '')) LIKE '%tops.co.th%'
    OR LOWER(COALESCE(catalog_search_url, '')) LIKE '%bigc.co.th%'
    OR LOWER(COALESCE(catalog_search_url, '')) LIKE '%lotuss.com%'
    OR LOWER(COALESCE(catalog_search_url, '')) LIKE '%tops.co.th%'
    OR LOWER(COALESCE(notes, '')) LIKE '%fallback%'
  )
ORDER BY city, supermarket_name;

-- 2) Delete identified polluted rows
DELETE FROM city_supermarket
WHERE LOWER(city) <> 'bangkok'
  AND (
    LOWER(supermarket_name) IN ('big c', 'lotus''s', 'tops')
    OR LOWER(COALESCE(official_website, '')) LIKE '%bigc.co.th%'
    OR LOWER(COALESCE(official_website, '')) LIKE '%lotuss.com%'
    OR LOWER(COALESCE(official_website, '')) LIKE '%tops.co.th%'
    OR LOWER(COALESCE(catalog_search_url, '')) LIKE '%bigc.co.th%'
    OR LOWER(COALESCE(catalog_search_url, '')) LIKE '%lotuss.com%'
    OR LOWER(COALESCE(catalog_search_url, '')) LIKE '%tops.co.th%'
    OR LOWER(COALESCE(notes, '')) LIKE '%fallback%'
  );

-- 3) Verify cleanup
SELECT city, supermarket_name, official_website
FROM city_supermarket
WHERE LOWER(city) <> 'bangkok'
  AND (
    LOWER(COALESCE(official_website, '')) LIKE '%bigc.co.th%'
    OR LOWER(COALESCE(official_website, '')) LIKE '%lotuss.com%'
    OR LOWER(COALESCE(official_website, '')) LIKE '%tops.co.th%'
  );
