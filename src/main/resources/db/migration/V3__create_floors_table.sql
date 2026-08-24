CREATE TABLE floors (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    outline     geometry(Polygon) NOT NULL,
    height_min  DOUBLE PRECISION NOT NULL,
    height_max  DOUBLE PRECISION NOT NULL,
    building_id UUID NOT NULL REFERENCES buildings(id) ON DELETE CASCADE
);

CREATE INDEX idx_floors_building_id ON floors (building_id);
CREATE INDEX idx_floors_height ON floors (height_min, height_max);