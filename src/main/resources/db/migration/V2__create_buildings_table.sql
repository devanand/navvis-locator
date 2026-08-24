CREATE TABLE buildings (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    outline    geometry(Polygon) NOT NULL,
    height_min DOUBLE PRECISION NOT NULL,
    height_max DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_buildings_height ON buildings (height_min, height_max);
CREATE INDEX idx_buildings_outline ON buildings USING GIST (outline);