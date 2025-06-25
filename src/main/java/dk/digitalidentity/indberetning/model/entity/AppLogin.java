package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@Table(name = "app_logins")
public class AppLogin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Nullable
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "username")
    private String username;

    @Size(max = 255)
    @Column(name = "password")
    private String password;

    @OneToOne(optional = false, orphanRemoval = false)
    @JoinColumn(name = "person_id", nullable = false)
    @JsonIgnore
    private Person person;

}
