package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@Entity
@Table(name = "gps_coordinates")
@NoArgsConstructor
public class GpsCoordinate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(nullable = false, name = "latitude")
    private double latitude;

    @Column(nullable = false, name = "longitude")
    private double longitude;

    @Column(nullable = false, name = "waypoint")
    private boolean waypoint;

    @Column(nullable = false)
    private boolean startPoint;

    @Column(nullable = false)
    private boolean endPoint;

    @Column(nullable = false)
    private int pointNumber;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "report_id", nullable = true)
    private Report report;

    @Column(name = "address")
    private String address;

    public GpsCoordinate(double latitude, double longitude, boolean waypoint, String address, Report report, boolean startPoint, boolean endPoint, int pointNumber) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.waypoint = waypoint;
        this.address = address;
        this.report = report;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.pointNumber = pointNumber;
    }

    public String toString() {
        return "GpsCoordinate(id=" + id + ", latitude=" + latitude + ", longitude=" + longitude + ", isWaypoint=" + waypoint + ", streetAddress=" + address +", report=" + report.getId() + ")";
    }
}