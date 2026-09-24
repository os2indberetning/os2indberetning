ALTER TABLE orgunits ADD COLUMN exclude_from_max_distance_to_subtract BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE orgunits_aud ADD COLUMN exclude_from_max_distance_to_subtract BOOLEAN;
