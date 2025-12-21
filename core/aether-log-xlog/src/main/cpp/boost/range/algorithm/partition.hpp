//  Copyright Neil Groves 2009. Use, modification and
//  distribution is subject to the Boost Software License, Version
//  1.0. (See accompanying file LICENSE_1_0.txt or copy at
//  http://www.boost.org/LICENSE_1_0.txt)
//
//
// For more information, see http://www.boost.org/libs/range/
//
#ifndef BOOST_RANGE_ALGORITHM_PARTITION__HPP_INCLUDED
#define BOOST_RANGE_ALGORITHM_PARTITION__HPP_INCLUDED

#include <boost/concept_check.hpp>
#include <boost/range/begin.hpp>
#include <boost/range/end.hpp>
#include <boost/range/concepts.hpp>
#include <boost/range/detail/range_return.hpp>
#include <algorithm>

namespace aether_boost {} namespace boost = aether_boost; namespace aether_boost
{
    namespace range
    {

/// \brief template function partition
///
/// range-based version of the partition std algorithm
///
/// \pre ForwardRange is a model of the ForwardRangeConcept
template<class ForwardRange, class UnaryPredicate>
inline BOOST_DEDUCED_TYPENAME range_iterator<ForwardRange>::type
partition(ForwardRange& rng, UnaryPredicate pred)
{
    BOOST_RANGE_CONCEPT_ASSERT(( ForwardRangeConcept<ForwardRange> ));
    return std::partition(aether_boost::begin(rng),aether_boost::end(rng),pred);
}

/// \overload
template<class ForwardRange, class UnaryPredicate>
inline BOOST_DEDUCED_TYPENAME range_iterator<ForwardRange>::type
partition(const ForwardRange& rng, UnaryPredicate pred)
{
    BOOST_RANGE_CONCEPT_ASSERT(( ForwardRangeConcept<const ForwardRange> ));
    return std::partition(aether_boost::begin(rng),aether_boost::end(rng),pred);
}

// range_return overloads

/// \overload
template< range_return_value re, class ForwardRange,
          class UnaryPredicate >
inline BOOST_DEDUCED_TYPENAME range_return<ForwardRange,re>::type
partition(ForwardRange& rng, UnaryPredicate pred)
{
    BOOST_RANGE_CONCEPT_ASSERT(( ForwardRangeConcept<ForwardRange> ));
    return aether_boost::range_return<ForwardRange,re>::
        pack(std::partition(aether_boost::begin(rng), aether_boost::end(rng), pred), rng);
}

/// \overload
template< range_return_value re, class ForwardRange,
          class UnaryPredicate >
inline BOOST_DEDUCED_TYPENAME range_return<const ForwardRange,re>::type
partition(const ForwardRange& rng, UnaryPredicate pred)
{
    BOOST_RANGE_CONCEPT_ASSERT(( ForwardRangeConcept<const ForwardRange> ));
    return aether_boost::range_return<const ForwardRange,re>::
        pack(std::partition(aether_boost::begin(rng), aether_boost::end(rng), pred), rng);
}

    } // namespace range
    using range::partition;
} // namespace aether_boost {} namespace boost = aether_boost; namespace aether_boost

#endif // include guard
