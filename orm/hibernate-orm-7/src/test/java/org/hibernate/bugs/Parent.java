package org.hibernate.bugs;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Version;

@Entity
@Audited
public class Parent {

	@Id
	@GeneratedValue
	private Long id;

	@Version
	private Integer version;

	private String name;

	@ManyToMany(mappedBy = "linkedParents", fetch = FetchType.LAZY)
	private Set<Child> children = new HashSet<>();

	public Parent() {
	}

	public Parent(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public Integer getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}

	public Set<Child> getChildren() {
		return children;
	}
}
