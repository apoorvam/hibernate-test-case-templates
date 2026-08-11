package org.hibernate.bugs;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

@Entity
@Audited
public class Child {

	@Id
	@GeneratedValue
	private Long id;

	@Version
	private Integer version;

	private String name;

	/**
	 * Seeds an uninitialized Parent proxy into the session when the Child is loaded.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private Parent mainParent;

	/**
	 * Owning side; deleting the Child fires the collection-remove event whose elements
	 * Envers maps into the bidirectional collection-change revision for Parent.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	private Set<Parent> linkedParents = new HashSet<>();

	public Child() {
	}

	public Child(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Parent getMainParent() {
		return mainParent;
	}

	public void setMainParent(Parent mainParent) {
		this.mainParent = mainParent;
	}

	public Set<Parent> getLinkedParents() {
		return linkedParents;
	}
}
