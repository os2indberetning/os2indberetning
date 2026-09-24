CREATE OR REPLACE VIEW view_reports AS
SELECT
    r.id,
    r.person_id,
    r.status,
    r.created_date,
    r.drive_date,
    r.full_name,
    r.employee_number,
    r.purpose,
    r.km_rate_type,
    r.comment,
    r.distance,
    r.amount_to_reimburse,
    r.is_extra_distance,
    (CASE
        WHEN r.is_round_trip IS TRUE THEN ((r.raw_distance * 2) - r.distance)
        WHEN r.is_round_trip IS FALSE THEN (r.raw_distance - r.distance)
    END) AS extra_distance_amount,
    r.four_km_rule,
    r.user_comment,
    r.is_round_trip AS round_trip,
    CONCAT(p.first_name, ' ', p.last_name) AS approved_by_name,
    ou.short_description AS orgunit_initials,
    ou.long_description AS orgunit_name,
    route.route_geometry AS route_geometry,
    r.is_from_app AS from_app,
    r.potential_approvers,
    r.is_divergent_address AS divergent_address,
    r.starts_at_home,
    r.ends_at_home,
    r.processed_date,
    r.closed_date,
    el.error_code AS error_code
FROM reports r
LEFT JOIN routes route ON route.id = r.route_id
LEFT JOIN persons p ON r.approved_by_id = p.id
LEFT JOIN employments emp ON r.employment_id = emp.id
LEFT JOIN orgunits ou ON emp.orgunit_id = ou.id
LEFT JOIN error_logs el ON r.error_log_id = el.id;